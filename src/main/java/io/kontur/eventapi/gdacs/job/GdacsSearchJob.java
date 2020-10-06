package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.gdacs.client.GdacsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GdacsSearchJob implements Runnable {

    private final GdacsClient gdacsClient;

    @Autowired
    public GdacsSearchJob(GdacsClient gdacsClient) {
        this.gdacsClient = gdacsClient;
    }

    @Override
    public void run() {
        importGdacs();
    }

    private void importGdacs(){
        System.out.println(gdacsClient.getXml());
    }
}
