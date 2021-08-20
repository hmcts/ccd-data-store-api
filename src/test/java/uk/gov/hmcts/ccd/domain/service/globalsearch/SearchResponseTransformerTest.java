package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.global.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.search.global.ServiceLookup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchResponseTransformerTest {

    private final SearchResponseTransformer underTest = new SearchResponseTransformer();

    private final LocationRefData location1 = LocationRefData.builder()
        .locationId("L-1")
        .locationName("Location 1")
        .regionId("R-1")
        .regionName("Region 1")
        .build();
    private final LocationRefData location2 = LocationRefData.builder()
        .locationId("L-2")
        .locationName("Location 2")
        .regionId("R-2")
        .regionName("Region 2")
        .build();

    private final ServiceRefData service1 = ServiceRefData.builder()
        .serviceCode(1L)
        .serviceShortDescription("Service 1")
        .build();
    private final ServiceRefData service2 = ServiceRefData.builder()
        .serviceCode(2L)
        .serviceShortDescription("Service 2")
        .build();

    @Test
    void testLocationLookupBuilderFunction() {
        // GIVEN
        final List<LocationRefData> refData = List.of(location1, location2);

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
        final List<ServiceRefData> refData = List.of(this.service1, service2);

        // WHEN
        final ServiceLookup serviceLookup = SearchResponseTransformer.SERVICE_LOOKUP_FUNCTION.apply(refData);

        // THEN
        assertThat(serviceLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getServiceShortDescription(1L)).isEqualTo("Service 1");
                assertThat(dictionary.getServiceShortDescription(2L)).isEqualTo("Service 2");
                assertThat(dictionary.getServiceShortDescription(3L)).isNull();
            });
    }
}
