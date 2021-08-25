package uk.gov.hmcts.ccd.domain.model.refdata;

import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

public class ServiceLookup {
    private final Map<String, String> servicesMap;

    public ServiceLookup(@NonNull final Map<String, String> servicesMap) {
        this.servicesMap = servicesMap;
    }

    public String getServiceShortDescription(final String serviceCode) {
        return Optional.ofNullable(serviceCode).map(servicesMap::get).orElse(null);
    }
}
