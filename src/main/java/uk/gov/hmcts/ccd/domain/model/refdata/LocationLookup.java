package uk.gov.hmcts.ccd.domain.model.refdata;

import java.util.HashMap;
import java.util.Map;

public class LocationLookup {
    private final Map<String, String> locationsMap = new HashMap<>();
    private final Map<String, String> regionsMap = new HashMap<>();

    public void add(final BuildingLocation buildingLocation) {
        regionsMap.put(buildingLocation.getRegionId(), buildingLocation.getRegion());
        locationsMap.put(buildingLocation.getBuildingLocationId(), buildingLocation.getBuildingLocationName());
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
