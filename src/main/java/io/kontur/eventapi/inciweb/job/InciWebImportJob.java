package io.kontur.eventapi.inciweb.job;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.inciweb.converter.InciWebXmlParser;
import io.kontur.eventapi.inciweb.dto.ParsedItem;
import io.kontur.eventapi.inciweb.service.InciWebService;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class InciWebImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(InciWebImportJob.class);
    public static PubDate XML_PUB_DATE = new PubDate(DateTimeUtil.uniqueOffsetDateTime());

    private final InciWebService inciWebService;
    private final InciWebXmlParser inciWebXmlParser;

    @Autowired
    protected InciWebImportJob(InciWebService inciWebService, InciWebXmlParser inciWebXmlParser, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.inciWebService = inciWebService;
        this.inciWebXmlParser = inciWebXmlParser;
        Gauge.builder("inciwebFeedXML", XML_PUB_DATE, (pub) -> pub.getPubDate().until(DateTimeUtil.uniqueOffsetDateTime(), ChronoUnit.HOURS))
                .description("InciWeb feed did not update (hours)")
                .register(meterRegistry);
    }

    @Override
    public void execute() throws Exception {
        Optional<String> xmlOpt = inciWebService.fetchXml();
        if (xmlOpt.isPresent()) {
            String xml = xmlOpt.get();
            try {
                setPubDate(xml);
                List<String> events = inciWebXmlParser.getItems(xml, "item");
                List<ParsedItem> parsedEvents = inciWebXmlParser.getParsedItems(events);
                List<DataLake> dataLakes = inciWebService.createDataLake(parsedEvents);
                inciWebService.saveEventsToDataLake(dataLakes);
            } catch (DateTimeParseException e) {
                LOG.error("Error while parsing pubDate from InciWeb");
            } catch (SAXException | ParserConfigurationException | XPathExpressionException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }

    }

    @Override
    public String getName() {
        return "inciWebImport";
    }

    private void setPubDate(String xml)
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        XML_PUB_DATE.setPubDate(inciWebXmlParser.getPubDate(xml));
    }

    @Data
    @AllArgsConstructor
    private static class PubDate {
        private OffsetDateTime pubDate;
    }
}
