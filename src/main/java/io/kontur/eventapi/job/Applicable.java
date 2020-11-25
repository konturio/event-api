package io.kontur.eventapi.job;

import java.util.List;
import java.util.stream.Collectors;

public interface Applicable<T> {

    default boolean isApplicable(T dataLakeDto) {
        return false;
    }

    default boolean isDefault() {
        return false;
    }

    static <Entry, Service extends Applicable<Entry>> Service get(List<Service> services, Entry entry) {
        List<Service> servicesForEntry = services.stream()
                .filter(service -> service.isApplicable(entry))
                .collect(Collectors.toList());

        if (servicesForEntry.size() > 1) {
            throw new IllegalStateException("found more then 1 service for entry: " + entry);
        }

        if (servicesForEntry.isEmpty()) {
            List<Service> defaultProvider = services.stream()
                    .filter(Applicable::isDefault)
                    .collect(Collectors.toList());

            if (defaultProvider.size() != 1) {
                throw new IllegalStateException("can not find service for entry: " + entry);
            }

            return defaultProvider.get(0);
        }

        return servicesForEntry.get(0);
    }
}
