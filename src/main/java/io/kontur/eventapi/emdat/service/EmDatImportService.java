package io.kontur.eventapi.emdat.service;

import io.kontur.eventapi.emdat.client.EmDatClient;
import io.kontur.eventapi.emdat.dto.EmDatAuthorizationResponse;
import io.kontur.eventapi.emdat.dto.EmDatFileStatisticResponse;
import io.kontur.eventapi.emdat.dto.EmDatPublicFile;
import io.kontur.eventapi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Component
public class EmDatImportService {

    private static final Logger LOG = LoggerFactory.getLogger(EmDatImportService.class);

    private final String loginQuery = "{\"variables\":{\"user\":\"%s\",\"password\":\"%s\"},\"query\":\"query login($user: String!, $password: String!) {  login(user: $user, password: $password)}\"}";
    private final String fileNameQuery = "{\"query\":\"mutation emdat_public() {  emdat_public() {count xlsx}}\"}";

    @Value("${emdat.user}")
    private String user;

    @Value("${emdat.password}")
    private String password;

    private final EmDatClient client;

    public EmDatImportService(EmDatClient client) {
        this.client = client;
    }

    public String obtainAuthToken() {
        String query = String.format(loginQuery, user, password);
        String json = client.graphqlApi(query, Collections.emptyMap());
        var response = JsonUtil.readJson(json, EmDatAuthorizationResponse.class);
        return response.getToken();
    }

    public EmDatPublicFile obtainFileStatistic(String token) {
        String json = client.graphqlApi(fileNameQuery, Collections.singletonMap("auth", token));
        var response = JsonUtil.readJson(json, EmDatFileStatisticResponse.class);
        return response.getEmDatPublicFile();
    }

    public InputStream obtainFile(String fileName, String token) {
        try {
            ByteArrayResource body = client.downloadFile(fileName, Collections.singletonMap("auth", token)).getBody();
            if (body != null) {
                return body.getInputStream();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return InputStream.nullInputStream();
    }
}
