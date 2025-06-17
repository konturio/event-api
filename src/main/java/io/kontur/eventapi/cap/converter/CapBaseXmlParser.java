package io.kontur.eventapi.cap.converter;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

import io.kontur.eventapi.cap.dto.CapParsedEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class CapBaseXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(CapBaseXmlParser.class);
    protected static final String DEFAULT_NS = "*";
    protected static final String PUBDATE = "pubDate";

    public OffsetDateTime getPubDate(String xml)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        var xmlDocument = getXmlDocument(xml);
        var xPath = XPathFactory.newInstance().newXPath();
        String pathToPubDate = "/rss/channel/pubDate/text()";
        var pubDateString = (String) xPath.compile(pathToPubDate).evaluate(xmlDocument, XPathConstants.STRING);
        return parseDateTimeFromString(pubDateString);
    }

    public Map<String, String> getItems(String xml) throws IOException, SAXException, ParserConfigurationException {
        return getItems(xml, getItemName(), getNamespace(), getIdTagName());
    }

    public Map<String, String> getItems(String xml, String itemName, String namespace, String idTagName)
            throws IOException, SAXException, ParserConfigurationException {
        Map<String, String> items = new HashMap<>();
        Document xmlDocument = getXmlDocument(xml);
        NodeList nodeList = xmlDocument.getElementsByTagNameNS(namespace, itemName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            try {
                Element element = (Element) nodeList.item(i);
                String id = String.valueOf(element.getElementsByTagNameNS(namespace, idTagName).item(0).getTextContent());
                if (StringUtils.isEmpty(id)) {
                    id = UUID.randomUUID().toString();
                }
                StringWriter writer = new StringWriter();

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(nodeList.item(i)), new StreamResult(writer));

                items.put(id, writer.toString());
            } catch (TransformerException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return items;
    }

    public Map<String, CapParsedEvent> getParsedItems(Map<String, String> itemsXml, String provider) {
        Map<String, CapParsedEvent> parsedItems = new HashMap<>();
        if (!CollectionUtils.isEmpty(itemsXml)) {
            for (String itemXml : itemsXml.keySet()) {
                getParsedItemForDataLake(itemsXml.get(itemXml), provider)
                        .ifPresent(parsedItem -> parsedItems.put(itemXml, parsedItem));
            }
        }
        return parsedItems;
    }

    public abstract Optional<CapParsedEvent> getParsedItemForDataLake(String xml, String provider);

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

    protected String getValueByTagName(Document xmlDocument, String tagName) {
        return getValueByTagName(xmlDocument, tagName, DEFAULT_NS);
    }

    protected String getValueByTagName(Document xmlDocument, String tagName, String namespace) {
        NodeList nodeList = xmlDocument.getElementsByTagNameNS(namespace, tagName);
        if (nodeList.getLength() == 0) {
            return "";
        }
        if (nodeList.getLength() > 1) {
            LOG.warn("Event contains more than one tag '{}'", tagName);
        }
        return nodeList.item(0).getTextContent();
    }

    protected String getItemName() {
        return "item";
    }

    protected String getNamespace() {
        return DEFAULT_NS;
    }

    protected String getIdTagName() {
        return "guid";
    }
}
