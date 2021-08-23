package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SearchResponseTransformerTest {
    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @InjectMocks
    private SearchResponseTransformer underTest;

    private final BuildingLocation location1 = BuildingLocation.builder()
        .buildingLocationId("L-1")
        .buildingLocationName("Location 1")
        .regionId("R-1")
        .region("Region 1")
        .build();
    private final BuildingLocation location2 = BuildingLocation.builder()
        .buildingLocationId("L-2")
        .buildingLocationName("Location 2")
        .regionId("R-2")
        .region("Region 2")
        .build();

    private final Service service1 = Service.builder()
        .serviceCode("SC1")
        .serviceShortDescription("Service 1")
        .build();
    private final Service service2 = Service.builder()
        .serviceCode("SC2")
        .serviceShortDescription("Service 2")
        .build();

    @Test
    void testLocationLookupBuilderFunction() {
        // GIVEN
        final List<BuildingLocation> refData = List.of(location1, location2);

        // WHEN
        final LocationLookup locationLookup = SearchResponseTransformer.LOCATION_LOOKUP_FUNCTION.apply(refData);

        // THEN
        assertThat(locationLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getLocationName("L-1")).isEqualTo("Location 1");
                assertThat(dictionary.getRegionName("R-2")).isEqualTo("Region 2");
                assertThat(dictionary.getRegionName("R-0")).isNull();
            });
    }

    @Test
    void testServiceLookupBuilderFunction() {
        // GIVEN
        final List<Service> refData = List.of(this.service1, service2);

        // WHEN
        final ServiceLookup serviceLookup = SearchResponseTransformer.SERVICE_LOOKUP_FUNCTION.apply(refData);

        // THEN
        assertThat(serviceLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getServiceShortDescription("SC1")).isEqualTo("Service 1");
                assertThat(dictionary.getServiceShortDescription("SC2")).isEqualTo("Service 2");
                assertThat(dictionary.getServiceShortDescription("SC3")).isNull();
            });
    }
}
