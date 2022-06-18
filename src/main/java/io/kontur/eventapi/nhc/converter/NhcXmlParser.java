package io.kontur.eventapi.nhc.converter;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;

import io.kontur.eventapi.converter.BaseXmlParser;
import io.kontur.eventapi.dto.ParsedEvent;
import io.kontur.eventapi.dto.ParsedItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Component
public class NhcXmlParser extends BaseXmlParser {
    private final static Logger LOG = LoggerFactory.getLogger(NhcXmlParser.class);

    @Override
    public Optional<ParsedEvent> getParsedItemForDataLake(String xml) {
        ParsedItem item = new ParsedItem();
        try {
            Document xmlDocument = getXmlDocument(xml);
            item.setGuid(getValueByTagName(xmlDocument, getIdTagName()));
            item.setPubDate(parseDateTimeFromString(getValueByTagName(xmlDocument, PUBDATE)));
            item.setData(xml);
            return Optional.of(item);
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from NHC events list. {}",
                    StringUtils.isNotBlank(item.getGuid()) ? item.getGuid() : "unknown");
            return Optional.empty();
        }

    }

}
