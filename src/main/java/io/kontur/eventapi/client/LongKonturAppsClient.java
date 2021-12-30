package io.kontur.eventapi.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;

@Qualifier("longKonturAppsClient")
@FeignClient(value = "longKonturAppsClient", url = "${konturApps.host}", name = "longKonturAppsClient")
public interface LongKonturAppsClient extends KonturAppsClient {
}
