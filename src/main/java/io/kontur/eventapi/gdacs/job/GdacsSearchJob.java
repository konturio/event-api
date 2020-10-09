package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.gdacs.service.GdacsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class GdacsSearchJob implements Runnable {

    private final GdacsClient gdacsClient;
    private final DataLakeDao dataLakeDao;
    private final GdacsService gdacsService;

    @Autowired
    public GdacsSearchJob(GdacsClient gdacsClient, DataLakeDao dataLakeDao, GdacsService gdacsService) {
        this.gdacsClient = gdacsClient;
        this.dataLakeDao = dataLakeDao;
        this.gdacsService = gdacsService;
    }

    @Override
    public void run() {
        try {
            String xml = gdacsClient.getXml();
            List<String> links = getLinks(xml);
            List<String> alerts = getAlerts(links);
            List<AlertForInsertDataLake> alertsForDataLakeSorted = getAlertsForDateLake(alerts);
            saveAlerts(alertsForDataLakeSorted);
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    private List<String> getLinks(String xml) throws ParserConfigurationException, IOException, SAXException {
        var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
        var rssNode = document.getDocumentElement();
        var channelNode = rssNode.getFirstChild();
        var channelChildNodes = channelNode.getChildNodes();
        var links = new ArrayList<String>();

        for (int i = 0; i < channelChildNodes.getLength(); i++) {
            Node nodeInChannel = channelChildNodes.item(i);
            if (nodeInChannel.getNodeName().equals("item")) {
                NodeList itemsXml = nodeInChannel.getChildNodes();
                for (int j = 0; j < itemsXml.getLength(); j++) {
                    Node nodeInItem = itemsXml.item(j);
                    if (nodeInItem.getNodeName().equals("link")) {
                        links.add(nodeInItem.getTextContent().replace("https://www.gdacs.org", ""));
                    }
                }
            }
        }
        return links;
    }

    private List<String> getAlerts(List<String> links) {
        return links.stream()
                .map(gdacsClient::getAlertByLink)
                .map(a -> a.substring(1))
                .collect(toList());
    }

    private List<AlertForInsertDataLake> getAlertsForDateLake(List<String> alerts) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var alertsForDataLake = new ArrayList<AlertForInsertDataLake>();
        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();

        for(String alert: alerts) {
            var inputStream = new ByteArrayInputStream(alert.getBytes());
            var xmlDocument = builder.parse(inputStream);
            var xPath = XPathFactory.newInstance().newXPath();

            String pathToExternalId = "/alert/identifier/text()";
            String pathToDateModified = "/alert/info/parameter[19]/value/text()";

            var externalId = (String) xPath.compile(pathToExternalId).evaluate(xmlDocument, XPathConstants.STRING);
            var updateDateString = (String) xPath.compile(pathToDateModified).evaluate(xmlDocument, XPathConstants.STRING);

            var updateDateOffset = ZonedDateTime
                    .parse(updateDateString, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toOffsetDateTime();

            alertsForDataLake.add(new AlertForInsertDataLake(
                    updateDateOffset,
                    externalId,
                    alert
            ));
        }
        alertsForDataLake.sort(Comparator.comparing(AlertForInsertDataLake::getUpdateDate));
        return alertsForDataLake;
    }

    private void saveAlerts(List<AlertForInsertDataLake> alerts){

        alerts.forEach(alert -> {
            var dataLakes = dataLakeDao.getDataLakeByExternalIdAndUpdateDate(
                    alert.getExternalId(),
                    alert.getUpdateDate()
            );
            if(dataLakes.isEmpty()){
                gdacsService.saveGdacs(alert);
            }
        });

    }
}




