package io.kontur.eventapi.nhc.converter;

import static io.kontur.eventapi.nhc.NhcUtil.MAIN_REGEXP;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Map;
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
public class NhcXmlParser extends CapBaseXmlParser {
    private final static Logger LOG = LoggerFactory.getLogger(NhcXmlParser.class);

    public static final String GUID = "guid";
    private static final String PUBDATE = "pubDate";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String LINK = "link";


    @Override
    public Optional<CapParsedEvent> getParsedItemForDataLake(String xml, String provider) {
        CapParsedItem item = new CapParsedItem();
        try {
            Document xmlDocument = getXmlDocument(xml);
            item.setGuid(getValueByTagName(xmlDocument, getIdTagName()));
            String pubDate = getValueByTagName(xmlDocument, PUBDATE);
            if (StringUtils.isNotBlank(pubDate)) {
                item.setPubDate(parseDateTimeFromString(getValueByTagName(xmlDocument, PUBDATE)));
                item.setData(xml);
                return Optional.of(item);
            } else {
                return Optional.empty();
            }
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from {} events list. {}", provider,
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
            return Optional.of(item);
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from NHC events list. {}",
                    StringUtils.isNotBlank(item.getGuid()) ? item.getGuid() : "unknown");
            return Optional.empty();
        }
    }

    public Map<Integer, Map<Integer, String>> parseDescription(String description) {
        String desc = substringBetween(description, "<pre>","</pre>")
                .replaceAll("\\n", " ")
                .replaceAll("\\r", " ")
                .replaceAll("\\s+", " ");
        return parseByPattern(desc, MAIN_REGEXP);
    }

}
