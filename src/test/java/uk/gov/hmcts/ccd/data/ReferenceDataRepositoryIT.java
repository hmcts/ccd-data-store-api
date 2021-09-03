package uk.gov.hmcts.ccd.data;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.CourtVenue;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_PATH;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_PATH;

class ReferenceDataRepositoryIT extends WireMockBaseTest implements ReferenceDataTestFixtures {

    @Inject
    private ReferenceDataRepository underTest;

    @BeforeEach
    void prepare() {
        List.of("buildingLocations", "orgServices")
            .parallelStream()
            .forEach(cacheName -> underTest.invalidateCache(cacheName));

        List.of(BUILDING_LOCATIONS_STUB_ID, SERVICES_STUB_ID).forEach(id -> {
            final Optional<StubMapping> stubMapping = Optional.ofNullable(wireMockServer.getSingleStubMapping(id));
            stubMapping.ifPresent(mapping -> wireMockServer.removeStub(mapping));
        });
        wireMockServer.resetRequests();
    }

    @Test
    void testShouldGetBuildingLocationsSuccessfullyFromUpstream() {
        // GIVEN
        final List<BuildingLocation> expectedBuildingLocations = buildBuildingLocations();
        stubBuildingLocationsSuccess(expectedBuildingLocations);

        // WHEN
        final List<BuildingLocation> actualBuildingLocations = underTest.getBuildingLocations();

        // THEN
        assertThat(actualBuildingLocations)
            .isNotEmpty()
            .hasSameElementsAs(expectedBuildingLocations);

        verifyWireMock(1, getRequestedFor(urlPathEqualTo(BUILDING_LOCATIONS_PATH)));
    }

    @Test
    void testShouldReturnEmptyWhenGetBuildingLocationsFromUpstreamFails() {
        // GIVEN
        stubUpstreamFault(BUILDING_LOCATIONS_PATH, BUILDING_LOCATIONS_STUB_ID);

        // WHEN
        final List<BuildingLocation> actualBuildingLocations = underTest.getBuildingLocations();

        // THEN
        assertThat(actualBuildingLocations)
            .isEmpty();

        verifyWireMock(1, getRequestedFor(urlPathEqualTo(BUILDING_LOCATIONS_PATH)));
    }

    @Test
    void testShouldGetServicesSuccessfullyFromUpstream() {
        // GIVEN
        final List<Service> expectedServices = buildServices();
        stubServicesSuccess(expectedServices);

        // WHEN
        final List<Service> actualServices = underTest.getServices();

        // THEN
        assertThat(actualServices)
            .isNotEmpty()
            .hasSameElementsAs(expectedServices);

        verifyWireMock(1, getRequestedFor(urlPathEqualTo(SERVICES_PATH)));
    }

    @Test
    void testShouldReturnEmptyWhenGetServicesFromUpstreamFails() {
        // GIVEN
        stubUpstreamFault(SERVICES_PATH, SERVICES_STUB_ID);

        // WHEN
        final List<Service> actualServices = underTest.getServices();

        // THEN
        assertThat(actualServices)
            .isEmpty();

        verifyWireMock(1, getRequestedFor(urlPathEqualTo(SERVICES_PATH)));
    }

    @Test
    void testShouldGetBuildingLocationsSuccessfullyFromCache() throws Exception {
        // GIVEN
        final List<BuildingLocation> cachedBuildingLocations = populateBuildingLocationsCache();

        // WHEN
        final List<BuildingLocation> actualBuildingLocations = underTest.getBuildingLocations();

        // THEN
        assertThat(actualBuildingLocations)
            .isNotEmpty()
            .hasSameElementsAs(cachedBuildingLocations);

        verifyWireMock(1, getRequestedFor(urlPathEqualTo(BUILDING_LOCATIONS_PATH)));
    }

    @Test
    void testShouldNotCacheEmptyBuildingLocations() throws Exception {
        // GIVEN
        attemptCachingEmptyBuildingLocations();

        // WHEN
        final List<BuildingLocation> actualBuildingLocations = underTest.getBuildingLocations();

        // THEN
        assertThat(actualBuildingLocations)
            .isEmpty();

        verifyWireMock(2, getRequestedFor(urlPathEqualTo(BUILDING_LOCATIONS_PATH)));
    }

    @Test
    void testShouldGetServicesSuccessfullyFromCache() throws Exception {
        // GIVEN
        final List<Service> cachedServices = populateServicesCache();

        // WHEN
        final List<Service> actualServices = underTest.getServices();

        // THEN
        assertThat(actualServices)
            .isNotEmpty()
            .hasSameElementsAs(cachedServices);

        verifyWireMock(1, getRequestedFor(urlPathEqualTo(SERVICES_PATH)));
    }

    @Test
    void testShouldNotCacheEmptyServices() throws Exception {
        // GIVEN
        attemptCachingEmptyServices();

        // WHEN
        final List<Service> actualServices = underTest.getServices();

        // THEN
        assertThat(actualServices)
            .isEmpty();

        verifyWireMock(2, getRequestedFor(urlPathEqualTo(SERVICES_PATH)));
    }

    private List<BuildingLocation> populateBuildingLocationsCache() throws Exception {
        stubBuildingLocationsSuccess(initialBuildingLocations);
        final List<BuildingLocation> cachedBuildingLocations = underTest.getBuildingLocations();
        SECONDS.sleep(1);

        return cachedBuildingLocations;
    }

    private void attemptCachingEmptyBuildingLocations() throws Exception {
        stubBuildingLocationsSuccess(emptyList());
        final List<BuildingLocation> buildingLocations = underTest.getBuildingLocations();

        assertThat(buildingLocations).isEmpty();

        SECONDS.sleep(1);
    }

    private List<Service> populateServicesCache() throws Exception {
        stubServicesSuccess(initialServices);
        final List<Service> cachedServices = underTest.getServices();
        SECONDS.sleep(1);

        return cachedServices;
    }

    private void attemptCachingEmptyServices() throws Exception {
        stubServicesSuccess(emptyList());
        final List<Service> services = underTest.getServices();

        assertThat(services).isEmpty();

        SECONDS.sleep(1);
    }

    private void stubBuildingLocationsSuccess(final List<BuildingLocation> buildingLocations) {
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(buildingLocations), BUILDING_LOCATIONS_STUB_ID);
    }

    private void stubServicesSuccess(final List<Service> services) {
        stubSuccess(SERVICES_PATH, objectToJsonString(services), SERVICES_STUB_ID);
    }

    private static List<BuildingLocation> buildBuildingLocations() {
        final CourtVenue courtVenue = CourtVenue.builder()
            .courtVenueId("123")
            .siteName("54 HAGLEY ROAD (BIRMINGHAM OFFICES)")
            .courtName("54 HAGLEY ROAD (BIRMINGHAM OFFICES)")
            .epimsId("815833")
            .openForPublic("YES")
            .courtTypeId("1")
            .courtType("North")
            .regionId("1")
            .region("Midlands")
            .clusterId("1")
            .clusterName("NBC")
            .courtStatus("Open")
            .postcode("B168PE")
            .courtAddress("HAGLEY ROAD")
            .phoneNumber("121680121011")
            .dxAddress("HAGLEY ROAD")
            .build();

        final BuildingLocation buildingLocation = BuildingLocation.builder()
            .buildingLocationId("123")
            .buildingLocationName("54 HAGLEY ROAD (BIRMINGHAM OFFICES)")
            .epimsId("815833")
            .buildingLocationStatus("LIVE")
            .area("North")
            .region("Midlands")
            .regionId("1")
            .clusterName("NBC")
            .clusterId("1")
            .postcode("B168PE")
            .address("HAGLEY ROAD")
            .courtVenues(List.of(courtVenue))
            .build();

        return List.of(buildingLocation);
    }

    private static List<Service> buildServices() {
        final Service service = Service.builder()
            .serviceId(11)
            .orgUnit("HMCTS")
            .businessArea("Civil, Family and Tribunals")
            .subBusinessArea("Civil and Family")
            .jurisdiction("Family")
            .serviceDescription("Probate")
            .lastUpdate(LocalDateTime.now())
            .serviceCode("ABA6")
            .serviceShortDescription("Probate")
            .ccdServiceName("Probate")
            .ccdCaseTypes(List.of(
                "Caveat",
                "GrantOfRepresentation",
                "LegacySearch",
                "PROBATE_ExceptionRecord",
                "StandingSearch",
                "WillLodgement"))
            .build();

        return List.of(service);
    }
}
