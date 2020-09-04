package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureWireMock(port = 18080, stubs = "classpath:mappings/PdcHazardImportJobIT.json")
class HpSrvSearchJobIT extends AbstractIntegrationTest {

    @Autowired
    private HpSrvSearchJob job;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Disabled
    public void uploadTestHazards() throws JsonProcessingException {
        job.run();

        verifyPagination();
        verifyMagsRequests();

        WireMock.resetAllRequests();

        job.run();

        verifyDownloadUpdatedHazardsSinceLastStart();
        verifyMagsRequests(); //since this test doesn't provide any mags, job should try to download them at every start
    }

    private void verifyPagination() throws JsonProcessingException {
        verify(3, postRequestedFor(urlEqualTo("/hp_srv/services/hazards/1/json/search_hazard")));

        List<LoggedRequest> requests = findAll(
                postRequestedFor(urlEqualTo("/hp_srv/services/hazards/1/json/search_hazard")));

        HpSrvSearchBody searchBody = objectMapper.readValue(requests.get(0).getBodyAsString(), HpSrvSearchBody.class);
        assertEquals(0, searchBody.getPagination().getOffset(), "offset=0 is expected at first request to hpSrv");

        searchBody = objectMapper.readValue(requests.get(1).getBodyAsString(), HpSrvSearchBody.class);
        assertEquals(3, searchBody.getPagination().getOffset(),
                "offset should be equal to previous offset value plus number of received hazards");

        searchBody = objectMapper.readValue(requests.get(2).getBodyAsString(), HpSrvSearchBody.class);
        assertEquals(5, searchBody.getPagination().getOffset(),
                "offset should be equal to previous offset value plus number of received hazards");
    }

    private void verifyMagsRequests() {
        verify(5, getRequestedFor(urlPathEqualTo("/hp_srv/services/mags/1/json/get_mags")));
    }

    private void verifyDownloadUpdatedHazardsSinceLastStart() throws JsonProcessingException {
        verify(1, postRequestedFor(urlEqualTo("/hp_srv/services/hazards/1/json/search_hazard")));

        List<LoggedRequest> requests = findAll(
                postRequestedFor(urlEqualTo("/hp_srv/services/hazards/1/json/search_hazard")));

        HpSrvSearchBody searchBody = objectMapper.readValue(requests.get(0).getBodyAsString(), HpSrvSearchBody.class);
        assertEquals("1594975736787", searchBody.getRestrictions().get(0).get(0).get("updateDate"));
        assertEquals("GREATER_THAN", searchBody.getRestrictions().get(0).get(0).get("searchType"));
    }
}