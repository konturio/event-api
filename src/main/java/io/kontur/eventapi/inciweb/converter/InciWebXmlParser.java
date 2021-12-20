package io.kontur.eventapi.inciweb.converter;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;

import io.kontur.eventapi.converter.BaseXmlParser;
import io.kontur.eventapi.inciweb.dto.ParsedItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Component
public class InciWebXmlParser extends BaseXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(InciWebXmlParser.class);

    private static final String GUID = "guid";
    private static final String PUBDATE = "pubDate";
    private static final String LONGITUDE = "long";
    private static final String LATITUDE = "lat";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String LINK = "link";

    public List<ParsedItem> getParsedItems(List<String> itemsXml) {
        List<ParsedItem> parsedItems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(itemsXml)) {
            for (String itemXml : itemsXml) {
                getParsedItemForDataLake(itemXml).ifPresent(parsedItems::add);
            }
        }
        return parsedItems;
    }

    public Optional<ParsedItem> getParsedItemForDataLake(String xml) {
        ParsedItem item = new ParsedItem();
        try {
            Document xmlDocument = getXmlDocument(xml);
            item.setGuid(getValueByTagName(xmlDocument, GUID));
            item.setPubDate(parseDateTimeFromString(getValueByTagName(xmlDocument, PUBDATE)));
            item.setData(xml);
            return Optional.of(item);
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from InciWeb events list. {}",
                    StringUtils.isNotBlank(item.getGuid()) ? item.getGuid() : "unknown");
            return Optional.empty();
        }
    }

    public Optional<ParsedItem> getParsedItem(String xml) {
        ParsedItem item = new ParsedItem();
        try {
            Document xmlDocument = getXmlDocument(xml);
            item.setGuid(getValueByTagName(xmlDocument, GUID));
            item.setPubDate(parseDateTimeFromString(getValueByTagName(xmlDocument, PUBDATE)));
            item.setTitle(getValueByTagName(xmlDocument, TITLE));
            item.setDescription(getValueByTagName(xmlDocument, DESCRIPTION));
            item.setLink(getValueByTagName(xmlDocument, LINK));
            item.setLongitude(Double.parseDouble(getValueByTagName(xmlDocument, LONGITUDE)));
            item.setLatitude(Double.parseDouble(getValueByTagName(xmlDocument, LATITUDE)));
            return Optional.of(item);
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from InciWeb events list. {}",
                    StringUtils.isNotBlank(item.getGuid()) ? item.getGuid() : "unknown");
            return Optional.empty();
        }
    }

}
