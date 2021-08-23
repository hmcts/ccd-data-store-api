package uk.gov.hmcts.ccd.domain.model.refdata;

import java.util.Map;

public class ServiceLookup {
    private final Map<String, String> servicesMap;

    public ServiceLookup(final Map<String, String> servicesMap) {
        this.servicesMap = servicesMap;
    }

    public String getServiceShortDescription(final String serviceCode) {
        return servicesMap.get(serviceCode);
    }
}
