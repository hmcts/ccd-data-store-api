package uk.gov.hmcts.ccd.data;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_PATH;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_PATH;

@TestPropertySource(locations = "classpath:cache-refresh-schedule.properties")
class ReferenceDataCacheRefreshIT extends WireMockBaseTest implements ReferenceDataTestFixtures {

    private final List<BuildingLocation> updatedBuildingLocations = ReferenceDataTestFixtures.buildingLocations("2");
    private final List<Service> updatedServices = ReferenceDataTestFixtures.services(22);

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

        verifyWiremockInvocation(BUILDING_LOCATIONS_PATH, 2);
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

        verifyWiremockInvocation(SERVICES_PATH, 2);
    }

    @Test
    void testShouldNotRefreshBuildingLocationsCacheWhenBuildingLocationsCannotBeRetrieved() {
        // GIVEN
        cacheContainsInitialReferenceData();
        referenceDataNotFoundUpstream();

        // WHEN/THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getBuildingLocations())
                .isNotEmpty()
                .hasSameElementsAs(initialBuildingLocations));
    }

    @Test
    void testShouldNotRefreshServicesCacheWhenServicesCannotBeRetrieved() {
        // GIVEN
        cacheContainsInitialReferenceData();
        referenceDataNotFoundUpstream();

        // WHEN/THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getServices())
                .isNotEmpty()
                .hasSameElementsAs(initialServices));
    }

    @Test
    void testThatExceptionShouldNotDisableCacheRefreshScheduler() {
        // GIVEN
        cacheContainsInitialReferenceData();

        // WHEN
        stubUpstreamFault(BUILDING_LOCATIONS_PATH, BUILDING_LOCATIONS_STUB_ID);

        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getBuildingLocations())
                .isNotEmpty()
                .hasSameElementsAs(initialBuildingLocations));

        updateUpstreamReferenceData();

        // THEN
        await()
            .pollDelay(1500, MICROSECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .untilAsserted(() -> assertThat(underTest.getBuildingLocations())
                .isNotEmpty()
                .hasSameElementsAs(updatedBuildingLocations));

        verifyWiremockInvocation(BUILDING_LOCATIONS_PATH, 3);
    }

    private void updateUpstreamReferenceData() {
        editSuccessStub(BUILDING_LOCATIONS_PATH,
            objectToJsonString(updatedBuildingLocations),
            BUILDING_LOCATIONS_STUB_ID);
        editSuccessStub(SERVICES_PATH, objectToJsonString(updatedServices), SERVICES_STUB_ID);
    }

    private void referenceDataNotFoundUpstream() {
        stubNotFound(BUILDING_LOCATIONS_PATH, BUILDING_LOCATIONS_STUB_ID);
        stubNotFound(SERVICES_PATH, SERVICES_STUB_ID);
    }

    private void verifyWiremockInvocation(final String path, final int count) {
        final RequestPattern requestPattern = getRequestedFor(urlPathEqualTo(path)).build();
        final VerificationResult verificationResult = wireMockServer.countRequestsMatching(requestPattern);
        assertThat(verificationResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCount()).isGreaterThanOrEqualTo(count));
    }

    private void cacheContainsInitialReferenceData() {
        stubSuccess(BUILDING_LOCATIONS_PATH, objectToJsonString(initialBuildingLocations), BUILDING_LOCATIONS_STUB_ID);
        stubSuccess(SERVICES_PATH, objectToJsonString(initialServices), SERVICES_STUB_ID);

        final List<BuildingLocation> cachedBuildingLocations = underTest.getBuildingLocations();
        final List<Service> cachedServices = underTest.getServices();

        assertThat(cachedBuildingLocations)
            .isNotEmpty();
        assertThat(cachedServices)
            .isNotEmpty();
    }
}
