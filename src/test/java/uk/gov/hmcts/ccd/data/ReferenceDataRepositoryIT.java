package uk.gov.hmcts.ccd.data;

import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.CourtVenue;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_PATH;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_PATH;

@TestPropertySource(locations = "classpath:cache-refresh-schedule.properties")
class ReferenceDataRepositoryIT extends WireMockBaseTest {

    private static final String BEARER = "Bearer ";
    private static final String SERVICE_AUTHORIZATION = "serviceauthorization";
    private static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODI2MDAyMzN9"
        + ".Lz467pTdzRF0MGQye8QDzoLLY_cxk79ZB3OOYdOR-0PGYK5sVay4lxOvhIa-1VnfizaaDDZUwmPdMwQOUBfpBQ";
    private static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;

    private final List<BuildingLocation> initialBuildingLocations = buildingLocations("1");
    private final List<BuildingLocation> updatedBuildingLocations = buildingLocations("2");
    private final List<Service> initialServices = services(11);
    private final List<Service> updatedServices = services(22);

    @Inject
    private ReferenceDataRepository underTest;

    @BeforeEach
    void clearCache() {
        List.of("buildingLocations", "orgServices")
            .parallelStream()
            .forEach(cacheName -> underTest.clearCache(cacheName));
    }

    @Test
    void testShouldGetBuildingLocationsSuccessfullyFromUpstream() {
        // GIVEN
        final List<BuildingLocation> expectedBuildingLocations = buildBuildingLocations();
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(expectedBuildingLocations));

        // WHEN
        final List<BuildingLocation> actualBuildingLocations = underTest.getBuildingLocations();

        // THEN
        assertThat(actualBuildingLocations)
            .isNotEmpty()
            .hasSameElementsAs(expectedBuildingLocations);
    }

    @Test
    void testShouldGetServicesSuccessfullyFromUpstream() {
        // GIVEN
        final List<Service> expectedServices = buildServices();
        stubSuccess(SERVICES_PATH, objectToJsonString(expectedServices));

        // WHEN
        final List<Service> actualServices = underTest.getServices();

        // THEN
        assertThat(actualServices)
            .isNotEmpty()
            .hasSameElementsAs(expectedServices);
    }

    @Test
    void testShouldGetBuildingLocationsSuccessfullyFromCache() {
        // GIVEN
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(initialBuildingLocations));
        final List<BuildingLocation> cachedBuildingLocations = underTest.getBuildingLocations();
        removeUpstreamReferenceData();

        // WHEN
        final List<BuildingLocation> actualBuildingLocations = underTest.getBuildingLocations();

        // THEN
        assertThat(actualBuildingLocations)
            .isNotEmpty()
            .hasSameElementsAs(cachedBuildingLocations);
    }

    @Test
    void testShouldGetServicesSuccessfullyFromCache() {
        // GIVEN
        stubSuccess(SERVICES_PATH, objectToJsonString(initialServices));
        final List<Service> cachedServices = underTest.getServices();
        removeUpstreamReferenceData();

        // WHEN
        final List<Service> actualServices = underTest.getServices();

        // THEN
        assertThat(actualServices)
            .isNotEmpty()
            .hasSameElementsAs(cachedServices);
    }

    @Test
    void testShouldRefreshBuildingLocationCacheOnScheduleSuccessfully() {
        // GIVEN
        cacheContainsInitialReferenceData();
        updateUpstreamReferenceData();

        // WHEN/THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getBuildingLocations())
                .isNotEmpty()
                .hasSameElementsAs(updatedBuildingLocations));
    }

    @Test
    void testShouldRefreshServicesCacheOnScheduleSuccessfully() {
        // GIVEN
        cacheContainsInitialReferenceData();
        updateUpstreamReferenceData();

        // WHEN/THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getServices())
                .isNotEmpty()
                .hasSameElementsAs(updatedServices));
    }

    @Test
    void testShouldNotRefreshBuildingLocationsCacheWhenBuildingLocationsCannotBeRetrieved() throws Exception {
        // GIVEN
        cacheContainsInitialReferenceData();
        unavailableUpstreamReferenceData();

        // WHEN/THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getBuildingLocations())
                .isNotEmpty()
                .hasSameElementsAs(initialBuildingLocations));
    }

    @Test
    void testShouldNotRefreshServicesCacheWhenServicesCannotBeRetrieved() throws Exception {
        // GIVEN
        cacheContainsInitialReferenceData();
        unavailableUpstreamReferenceData();

        // WHEN/THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getServices())
                .isNotEmpty()
                .hasSameElementsAs(initialServices));
    }

    private void updateUpstreamReferenceData() {
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(updatedBuildingLocations));
        stubSuccess(SERVICES_PATH, objectToJsonString(updatedServices));
    }

    private void removeUpstreamReferenceData() {
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(emptyList()));
        stubSuccess(SERVICES_PATH, objectToJsonString(emptyList()));
    }

    private void unavailableUpstreamReferenceData() {
        stubNotFound(BUILDING_LOCATIONS_PATH);
        stubNotFound(SERVICES_PATH);
    }

    private void cacheContainsInitialReferenceData() {
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(initialBuildingLocations));
        stubSuccess(SERVICES_PATH, objectToJsonString(initialServices));

        final List<BuildingLocation> cachedBuildingLocations = underTest.getBuildingLocations();
        final List<Service> cachedServices = underTest.getServices();

        assertThat(cachedBuildingLocations)
            .isNotEmpty();
        assertThat(cachedServices)
            .isNotEmpty();
    }

    private static List<BuildingLocation> buildingLocations(final String id) {
        return List.of(BuildingLocation.builder()
            .buildingLocationId(id)
            .build());
    }

    private static List<Service> services(final int id) {
        return List.of(Service.builder()
            .serviceId(id)
            .build());
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

    private void stubSuccess(final String path, final String payload) {
        stubFor(get(urlPathEqualTo(path))
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.OK.value())
                .withBody(payload)
            )
        );
    }

    private void stubNotFound(final String path) {
        stubFor(get(urlPathEqualTo(path))
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
            )
        );
    }
}
