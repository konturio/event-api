package io.kontur.eventapi.gdacs.job;

import com.google.common.base.Strings;
import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.gdacs.service.GdacsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class GdacsSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

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
            LOG.info("Gdacs import job has started");
            String xml = gdacsClient.getXml();
            List<String> links = getLinks(xml);
            List<String> alerts = getAlerts(links);
            List<AlertForInsertDataLake> alertsForDataLake = getAlertsForDateLake(alerts);
            saveAlerts(alertsForDataLake);
            LOG.info("Gdacs import job has finished");
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.warn("Gdacs import job has failed", e);
        }
    }

    List<String> getLinks(String xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var links = new ArrayList<String>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var inputStream = new ByteArrayInputStream(xml.getBytes());
        var xmlDocument = builder.parse(inputStream);
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToItems = "/rss/channel/item";

        var itemNodeList = (NodeList) xPath.compile(pathToItems).evaluate(xmlDocument, XPathConstants.NODESET);

        for(int i = 0; i < itemNodeList.getLength(); i++){
            int indexOfItems = i + 1;
            String pathToLink = "/rss/channel/item[" + indexOfItems + "]/link/text()";
            var link = (String) xPath.compile(pathToLink).evaluate(xmlDocument, XPathConstants.STRING);
            links.add(link.replace("https://www.gdacs.org", ""));
        }
        return links;
    }

    List<String> getAlerts(List<String> links) {
        return links.parallelStream()
                .map(this::getAlertAfterHandleException)
                .filter(alert -> !alert.isEmpty())
                .filter(alert -> !alert.contains("<!DOCTYPE html"))
                .map(alert -> alert.startsWith("\uFEFF") ? alert.substring(1) : alert)
                .collect(toList());
    }

    private String getAlertAfterHandleException(String link){
        try {
            return gdacsClient.getAlertByLink(link);
        } catch (FeignException e){
            LOG.warn("Alert by link https://testemops.pdc.org{} not found", link);
        }
        return "";
    }

    List<AlertForInsertDataLake> getAlertsForDateLake(List<String> alerts) throws ParserConfigurationException {
        var alertsForDataLake = new ArrayList<AlertForInsertDataLake>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();

        String pathToExternalId = "/alert/identifier/text()";
        String pathToDateModified = "/alert/info/parameter";

        for (String alert : alerts) {

            try {
                var inputStream = new ByteArrayInputStream(alert.getBytes());
                var xmlDocument = builder.parse(inputStream);
                var xPath = XPathFactory.newInstance().newXPath();

                var externalId = (String) xPath.compile(pathToExternalId).evaluate(xmlDocument, XPathConstants.STRING);
                if(Strings.isNullOrEmpty(externalId)) continue;

                var parameterNodeList = (NodeList) xPath.compile(pathToDateModified).evaluate(xmlDocument, XPathConstants.NODESET);

                for (int i = 0; i < parameterNodeList.getLength(); i++) {
                    int indexOfParameters = i + 1;
                    String pathToValueName = "/alert/info/parameter[" + indexOfParameters + "]/valueName/text()";
                    var valueName = (String) xPath.compile(pathToValueName).evaluate(xmlDocument, XPathConstants.STRING);

                    if (valueName.equals("datemodified")) {
                        String pathToUpdateDate = "/alert/info/parameter[" + indexOfParameters + "]/value/text()";
                        var updateDateString = (String) xPath.compile(pathToUpdateDate).evaluate(xmlDocument, XPathConstants.STRING);

                        var updateDateOffset = ZonedDateTime
                                .parse(updateDateString, DateTimeFormatter.RFC_1123_DATE_TIME)
                                .toOffsetDateTime();

                        alertsForDataLake.add(new AlertForInsertDataLake(
                                updateDateOffset,
                                externalId,
                                alert
                        ));
                        break;
                    }
                }
            } catch (IOException | SAXException | XPathExpressionException | DateTimeParseException e){
                LOG.warn("Alert is not valid: {}", alert);
            }
        }
        return alertsForDataLake;
    }

    void saveAlerts(List<AlertForInsertDataLake> alerts) {
        alerts.forEach(alert -> {
            var dataLakes = dataLakeDao.getDataLakeByExternalIdAndUpdateDate(
                    alert.getExternalId(),
                    alert.getUpdateDate()
            );
            if (dataLakes.isEmpty()) {
                gdacsService.saveGdacs(alert);
            }
        });
    }
}




