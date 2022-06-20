package io.kontur.eventapi.inciweb.converter;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;

import io.kontur.eventapi.cap.converter.CapBaseXmlParser;
import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Component
public class InciWebXmlParser extends CapBaseXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(InciWebXmlParser.class);

    public static final String GUID = "guid";

    private static final String PUBDATE = "pubDate";
    private static final String LONGITUDE = "long";
    private static final String LATITUDE = "lat";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String LINK = "link";

    @Override
    protected Document getXmlDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        return super.getXmlDocument(xml);
    }

    public Optional<CapParsedEvent> getParsedItemForDataLake(String xml, String provider) {
        CapParsedItem item = new CapParsedItem();
        try {
            Document xmlDocument = getXmlDocument(xml);
            item.setGuid(getValueByTagName(xmlDocument, getIdTagName()));
            item.setPubDate(parseDateTimeFromString(getValueByTagName(xmlDocument, PUBDATE)));
            item.setData(xml);
            return Optional.of(item);
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from InciWeb events list. {}",
                    StringUtils.isNotBlank(item.getGuid()) ? item.getGuid() : "unknown");
            return Optional.empty();
        }

    }

    public Optional<CapParsedItem> getParsedItem(String xml) {
        CapParsedItem item = new CapParsedItem();
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
