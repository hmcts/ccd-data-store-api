package uk.gov.hmcts.ccd.data;

import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;

import java.util.List;
import java.util.UUID;

public interface ReferenceDataTestFixtures {

    /*
     * These UUID values must also be used in wiremock mapping files which are used by other integration tests. see:
     *   /src/test/resources/mappings/refdata/get_building_locations.json
     *   /src/test/resources/mappings/refdata/get_org_services.json.
     */
    UUID BUILDING_LOCATIONS_STUB_ID = UUID.fromString("2D4CF82C-57E8-4D5D-B07D-74507EC00675");
    UUID SERVICES_STUB_ID = UUID.fromString("3A080490-1828-44C3-B50E-D7543CD6DFDC");

    List<BuildingLocation> initialBuildingLocations = buildingLocations("1");
    List<ServiceReferenceData> initialServices = services(11);

    static List<BuildingLocation> buildingLocations(final String id) {
        return List.of(BuildingLocation.builder()
            .epimmsId(id)
            .build());
    }

    static List<ServiceReferenceData> services(final int id) {
        return List.of(ServiceReferenceData.builder()
            .serviceId(id)
            .build());
    }

}
