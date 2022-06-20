package io.kontur.eventapi.cap.job;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import io.kontur.eventapi.cap.converter.CapBaseXmlParser;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.cap.service.CapImportService;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public abstract class CapImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(CapImportJob.class);

    public static PubDate XML_PUB_DATE = new PubDate(DateTimeUtil.uniqueOffsetDateTime());

    private final CapImportService service;
    private final CapBaseXmlParser xmlParser;
    private final DataLakeDao dataLakeDao;

    public CapImportJob(CapImportService service, CapBaseXmlParser xmlParser, DataLakeDao dataLakeDao,
                        MeterRegistry meterRegistry, String meterName, String meterDesc) {
        super(meterRegistry);
        this.service = service;
        this.xmlParser = xmlParser;
        this.dataLakeDao = dataLakeDao;
        Gauge.builder(meterName, XML_PUB_DATE, (pub) -> pub.getPubDate().until(DateTimeUtil.uniqueOffsetDateTime(), ChronoUnit.HOURS))
                .description(meterDesc)
                .register(meterRegistry);
    }

    public void execute() {
        Optional<String> xmlOpt = service.fetchXml(getName());
        if (xmlOpt.isPresent()) {
            String xml = xmlOpt.get();
            try {
                String preparedXml = prepareXml(xml);
                setPubDate(preparedXml);
                Map<String, String> events = xmlParser.getItems(preparedXml);
                Map<String, String> filteredEvents = filterItems(events);
                Map<String, CapParsedEvent> parsedEvents = xmlParser.getParsedItems(filteredEvents, getProvider());
                Map<String, CapParsedEvent> filteredParsedEvents = filterParsedItems(parsedEvents);
                List<DataLake> dataLakes = service.createDataLakes(filteredParsedEvents, getProvider());
                service.saveDataLakes(dataLakes);
            } catch (DateTimeParseException e) {
                LOG.error("Parsing pubDate from {} was failed", getName());
            } catch (SAXException | ParserConfigurationException | XPathExpressionException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    protected String prepareXml(String xml) {
        return xml;
    }

    @Override
    public String getName() {
        return "capImportJob";
    }

    protected abstract String getProvider();
    public Map<String, String> filterItems(Map<String, String> events) {
        return events;
    }

    protected Map<String, CapParsedEvent> filterParsedItems(Map<String, CapParsedEvent> parsedEvents) {
        return parsedEvents;
    }

    protected DataLakeDao getDataLakeDao() {
        return dataLakeDao;
    }

    protected void setPubDate(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException {
        XML_PUB_DATE.setPubDate(xmlParser.getPubDate(xml));
    }

    @Data @AllArgsConstructor
    protected static class PubDate {
        private OffsetDateTime pubDate;
    }
}




