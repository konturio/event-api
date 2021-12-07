package io.kontur.eventapi.converter;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public abstract class BaseXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(BaseXmlParser.class);
    private static final String DEFAULT_NS = "*";

    public OffsetDateTime getPubDate(String xml)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        var xmlDocument = getXmlDocument(xml);
        var xPath = XPathFactory.newInstance().newXPath();
        String pathToPubDate = "/rss/channel/pubDate/text()";
        var pubDateString = (String) xPath.compile(pathToPubDate).evaluate(xmlDocument, XPathConstants.STRING);
        return parseDateTimeFromString(pubDateString);
    }

    public List<String> getItems(String xml, String itemName)
            throws IOException, ParserConfigurationException, SAXException {
        return getItems(xml, itemName, DEFAULT_NS);
    }

    public List<String> getItems(String xml, String itemName, String namespace)
            throws IOException, SAXException, ParserConfigurationException {
        List<String> items = new ArrayList<>();
        Document xmlDocument = getXmlDocument(xml);
        NodeList nodeList = xmlDocument.getElementsByTagNameNS(namespace, itemName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            try {
                StringWriter writer = new StringWriter();

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(nodeList.item(i)), new StreamResult(writer));

                items.add(writer.toString());
            } catch (TransformerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return items;
    }

    protected Document getXmlDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return builder.parse(inputStream);
    }

    protected Map<String, String> parseParameters(NodeList parameterNodes, Set<String> parameterNames, String keyName,
                                                String valueName) {
        Map<String, String> parameters = parameterNames.stream().map(name -> Map.entry(name, ""))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (int i = 0; i < parameterNodes.getLength(); i++) {
            NodeList parameterChildNodes = parameterNodes.item(i).getChildNodes();
            String parameterName = getNodeValueByName(parameterChildNodes, keyName);
            if (parameters.containsKey(parameterName)) {
                parameters.replace(parameterName, getNodeValueByName(parameterChildNodes, valueName));
            }
        }
        return parameters;
    }

    protected String getNodeValueByName(NodeList nodes, String childNodeName) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (StringUtils.equals(node.getLocalName(), childNodeName)) {
                return node.getTextContent();
            }
        }
        return "";
    }

    protected String getValueByTagName(String xml, Document xmlDocument, String tagName) {
        return getValueByTagName(xml, xmlDocument, tagName, DEFAULT_NS);
    }

    protected String getValueByTagName(String xml, Document xmlDocument, String tagName, String namespace) {
        NodeList nodeList = xmlDocument.getElementsByTagNameNS(namespace, tagName);
        if (nodeList.getLength() == 0) {
            LOG.warn("Event does not contain tag '{}': \n" + xml, tagName);
            return "";
        }
        if (nodeList.getLength() > 1) {
            LOG.warn("Event contains more than one tag '{}': \n" + xml, tagName);
        }
        return nodeList.item(0).getTextContent();
    }
}
