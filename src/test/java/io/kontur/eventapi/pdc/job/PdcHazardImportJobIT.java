package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 18080)
class PdcHazardImportJobIT {

    @Autowired
    private PdcHazardImportJob job;

    @Autowired
    private HpSrvClient hpSrvClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void uploadTestHazards() throws JsonProcessingException {
        job.run();

        verifyPagination();
    }

    private void verifyPagination() throws JsonProcessingException {
        List<ServeEvent> allServeEvents = new ArrayList<>(getAllServeEvents());
        assertEquals(3, allServeEvents.size(), "3 requests to hpSrv API are expected");

        allServeEvents.sort(Comparator.comparing(o -> o.getRequest().getLoggedDate()));

        HpSrvSearchBody searchBody = objectMapper.readValue(allServeEvents.get(0).getRequest().getBodyAsString(), HpSrvSearchBody.class);
        assertEquals(0, searchBody.getPagination().getOffset(), "offset=0 is expected at first request to hpSrv");

        searchBody = objectMapper.readValue(allServeEvents.get(1).getRequest().getBodyAsString(), HpSrvSearchBody.class);
        assertEquals(3, searchBody.getPagination().getOffset(),
                "offset should be equal to previous offset value plus number of received hazards");

        searchBody = objectMapper.readValue(allServeEvents.get(2).getRequest().getBodyAsString(), HpSrvSearchBody.class);
        assertEquals(5, searchBody.getPagination().getOffset(),
                "offset should be equal to previous offset value plus number of received hazards");
    }

}