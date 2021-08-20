package uk.gov.hmcts.ccd.domain.model.search.global;

import uk.gov.hmcts.ccd.domain.service.globalsearch.LocationRefData;

import java.util.HashMap;
import java.util.Map;

public class LocationLookup {
    private final Map<String, String> locationsMap = new HashMap<>();
    private final Map<String, String> regionsMap = new HashMap<>();

    public void add(final LocationRefData locationRefData) {
        regionsMap.put(locationRefData.getRegionId(), locationRefData.getRegionName());
        locationsMap.put(locationRefData.getLocationId(), locationRefData.getLocationName());
    }

    public String getLocationName(final String locationId) {
        return locationsMap.get(locationId);
    }

    public String getRegionName(final String regionId) {
        return regionsMap.get(regionId);
    }

    public LocationLookup combine(final LocationLookup other) {
        this.regionsMap.putAll(other.regionsMap);
        this.locationsMap.putAll(other.locationsMap);

        return this;
    }
}
