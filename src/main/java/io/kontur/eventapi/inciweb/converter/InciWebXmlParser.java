package io.kontur.eventapi.inciweb.converter;

import static io.kontur.eventapi.util.DateTimeUtil.parseZonedDateTimeByFormatter;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;

import io.kontur.eventapi.cap.converter.CapBaseXmlParser;
import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.inciweb.InciWebUtil;
import io.kontur.eventapi.util.DateTimeUtil;
import liquibase.repackaged.org.apache.commons.collections4.MapUtils;
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
            item.setPubDate(parseZonedDateTimeByFormatter(
                    getValueByTagName(xmlDocument, PUBDATE), DateTimeUtil.ZONED_DATETIME_FORMATTER));
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
            item.setPubDate(parseZonedDateTimeByFormatter(
                    getValueByTagName(xmlDocument, PUBDATE), DateTimeUtil.ZONED_DATETIME_FORMATTER));
            item.setTitle(getValueByTagName(xmlDocument, TITLE));
            String description = getValueByTagName(xmlDocument, DESCRIPTION);
            item.setDescription(description);
            item.setLink(getValueByTagName(xmlDocument, LINK));

            // get coordinates from description
            Map<Integer, Map<Integer, String>> matchers = parseByPattern(description, InciWebUtil.COORDINATES_REGEXP);
            if (MapUtils.isNotEmpty(matchers) && MapUtils.isNotEmpty(matchers.get(1))) {
                Map<Integer, String> matches = matchers.get(1);
                if (StringUtils.isNotBlank(matches.get(InciWebUtil.LAT_DEGREE))
                        && StringUtils.isNotBlank(matches.get(InciWebUtil.LON_DEGREE))) {
                    item.setLatitude(parseAndConvertCoordinateToDouble(matches, InciWebUtil.LAT_DEGREE,
                            InciWebUtil.LAT_MINUTES, InciWebUtil.LAT_SECONDS, "latitude", item.getGuid()));
                    item.setLongitude(parseAndConvertCoordinateToDouble(matches, InciWebUtil.LON_DEGREE,
                            InciWebUtil.LON_MINUTES, InciWebUtil.LON_SECONDS, "longitude", item.getGuid()));
                }
            }
            return Optional.of(item);
        } catch (ParserConfigurationException | IOException | SAXException | DateTimeParseException | NumberFormatException e) {
            LOG.error("Error while parsing item from InciWeb events list. {}",
                    StringUtils.isNotBlank(item.getGuid()) ? item.getGuid() : "unknown");
            return Optional.empty();
        }
    }

    private Double parseAndConvertCoordinateToDouble(Map<Integer, String> source, Integer degreePos, Integer minPos,
                                                     Integer secPos, String coordName, String guid) {
        try {
            String coord_deg = StringUtils.isNotBlank(source.get(degreePos)) ? source.get(degreePos) : "0";
            String coord_min = StringUtils.isNotBlank(source.get(minPos)) ? source.get(minPos) : "0";
            String coord_sec = StringUtils.isNotBlank(source.get(secPos)) ? source.get(secPos) : "0";
            if (coord_deg.startsWith("-")) {
                return Double.parseDouble(coord_deg) - Double.parseDouble(coord_min) / 60
                        - Double.parseDouble(coord_sec) / 3600;
            } else {
                return Double.parseDouble(coord_deg) + Double.parseDouble(coord_min) / 60
                        + Double.parseDouble(coord_sec) / 3600;
            }
        } catch (Exception e) {
            LOG.warn("Can't parse {} for InciWeb event {}", coordName, guid);
        }
        return null;
    }
}
