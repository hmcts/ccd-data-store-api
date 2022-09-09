package uk.gov.hmcts.ccd.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE_KEY;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE_KEY;


@DisplayName("ReferenceDataRepository")
@ExtendWith(MockitoExtension.class)
class ReferenceDataRepositoryTest {

    private static final String TEST_REF_DATA_URL = "TEST_DATA_URl";

    private static final String BEARER_TEST_JWT = "Bearer testJwt";
    private static final String SERVICE_JWT = "eyAidGVzdCI6InRlc3QiIH0=";

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    protected CacheManager cacheManager;

    private HttpHeaders systemUserHttpHeaders;

    @Captor
    ArgumentCaptor<HttpEntity<?>> entityCaptor;

    @InjectMocks
    private ReferenceDataRepository underTest;

    @BeforeEach
    void setUp() {
        systemUserHttpHeaders = new HttpHeaders();
        systemUserHttpHeaders.add("Authorization", BEARER_TEST_JWT);
        systemUserHttpHeaders.add("ServiceAuthorization", SERVICE_JWT);
        when(securityUtils.authorizationHeadersForDataStoreSystemUser()).thenReturn(systemUserHttpHeaders);

        when(applicationParams.getReferenceDataApiUrl()).thenReturn(TEST_REF_DATA_URL);
    }

    @DisplayName("getBuildingLocations()")
    @Nested
    class GetBuildingLocations {

        @Test
        @DisplayName("Should load ReferenceData using SystemUser headers")
        void shouldLoadReferenceDataUsingSystemUserHeaders() {

            // GIVEN
            BuildingLocation[] expectedResponse = new BuildingLocation[]{
                BuildingLocation.builder().build()
            };
            configMockRestTemplate(ReferenceDataRepository.BUILDING_LOCATIONS_PATH, expectedResponse);

            // WHEN
            List<BuildingLocation> result = underTest.getBuildingLocations();

            // THEN
            verifyApiCallSignature(ReferenceDataRepository.BUILDING_LOCATIONS_PATH, expectedResponse.getClass());
            assertEquals(expectedResponse.length, result.size());
        }

        @ParameterizedTest(name = "Should return empty result for empty or null ReferenceData response: {0}")
        @NullAndEmptySource
        void shouldReturnEmptyResultForEmptyOrNullReferenceDataResponse(BuildingLocation[] referenceDataResponse) {

            // GIVEN
            configMockRestTemplate(ReferenceDataRepository.BUILDING_LOCATIONS_PATH, referenceDataResponse);

            // WHEN
            List<BuildingLocation> result = underTest.getBuildingLocations();

            // THEN
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should return empty result after exception loading ReferenceData")
        void shouldReturnEmptyResultAfterReferenceDataException() {

            // GIVEN
            configMockRestTemplateThrowException(ReferenceDataRepository.BUILDING_LOCATIONS_PATH);

            // WHEN
            List<BuildingLocation> result = underTest.getBuildingLocations();

            // THEN
            assertEquals(0, result.size());
        }

    }

    @DisplayName("getServices()")
    @Nested
    class GetServices {

        @Test
        @DisplayName("Should load ReferenceData using SystemUser headers")
        void shouldLoadReferenceDataUsingSystemUserHeaders() {

            // GIVEN
            ServiceReferenceData[] expectedResponse = new ServiceReferenceData[]{
                ServiceReferenceData.builder().build()
            };
            configMockRestTemplate(ReferenceDataRepository.SERVICES_PATH, expectedResponse);

            // WHEN
            List<ServiceReferenceData> result = underTest.getServices();

            // THEN
            verifyApiCallSignature(ReferenceDataRepository.SERVICES_PATH, expectedResponse.getClass());
            assertEquals(expectedResponse.length, result.size());
        }

        @ParameterizedTest(name = "Should return empty result for empty or null ReferenceData response: {0}")
        @NullAndEmptySource
        void shouldReturnEmptyResultForEmptyOrNullReferenceDataResponse(ServiceReferenceData[] referenceDataResponse) {

            // GIVEN
            configMockRestTemplate(ReferenceDataRepository.SERVICES_PATH, referenceDataResponse);

            // WHEN
            List<ServiceReferenceData> result = underTest.getServices();

            // THEN
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should return empty result after exception loading ReferenceData")
        void shouldReturnEmptyResultAfterReferenceDataException() {

            // GIVEN
            configMockRestTemplateThrowException(ReferenceDataRepository.SERVICES_PATH);

            // WHEN
            List<ServiceReferenceData> result = underTest.getServices();

            // THEN
            assertEquals(0, result.size());
        }

    }


    @DisplayName("updateCache()")
    @Nested
    class UpdateCache {

        @Mock
        private Cache cache;

        @Captor
        private ArgumentCaptor<List<BuildingLocation>> buildingLocationsCaptor;

        @Captor
        private ArgumentCaptor<List<ServiceReferenceData>> serviceReferenceDataCaptor;

        @AfterEach
        public void tearDown() {
            cacheManager.getCacheNames().forEach(
                cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        }

        @Test
        @DisplayName("Should update BuildingLocationCache after successful call")
        void shouldUpdateBuildingLocationCacheAfterSuccessfulCall() {

            // GIVEN
            BuildingLocation[] expectedResponse = new BuildingLocation[]{
                BuildingLocation.builder().epimmsId("BUILDING-UPDATE-1").build(),
                BuildingLocation.builder().epimmsId("BUILDING-UPDATE-2").build()
            };
            configMockRestTemplate(ReferenceDataRepository.BUILDING_LOCATIONS_PATH, expectedResponse);
            configMockCache(BUILDING_LOCATIONS_CACHE);

            // WHEN
            underTest.updateBuildingLocationCache();

            // THEN
            verify(cache).putIfAbsent(eq(BUILDING_LOCATIONS_CACHE_KEY), buildingLocationsCaptor.capture());
            List<BuildingLocation> buildingLocations = buildingLocationsCaptor.getValue();
            assertNotNull(buildingLocations);
            assertEquals(expectedResponse.length, buildingLocations.size());
            assertThat(
                buildingLocations.stream().map(BuildingLocation::getEpimmsId).collect(Collectors.toList()),
                containsInAnyOrder("BUILDING-UPDATE-1", "BUILDING-UPDATE-2")
            );
        }

        @Test
        @DisplayName("Should update ServicesCache after successful call")
        void shouldUpdateServicesCacheAfterSuccessfulCall() {

            // GIVEN
            ServiceReferenceData[] expectedResponse = new ServiceReferenceData[]{
                ServiceReferenceData.builder().serviceCode("SERVICES-UPDATE-1").build(),
                ServiceReferenceData.builder().serviceCode("SERVICES-UPDATE-2").build()
            };
            configMockRestTemplate(ReferenceDataRepository.SERVICES_PATH, expectedResponse);
            configMockCache(SERVICES_CACHE);

            // WHEN
            underTest.updateServicesCache();

            // THEN
            verify(cache).putIfAbsent(eq(SERVICES_CACHE_KEY), serviceReferenceDataCaptor.capture());
            List<ServiceReferenceData> serviceReferenceData = serviceReferenceDataCaptor.getValue();
            assertNotNull(serviceReferenceData);
            assertEquals(expectedResponse.length, serviceReferenceData.size());
            assertThat(
                serviceReferenceData.stream().map(ServiceReferenceData::getServiceCode).collect(Collectors.toList()),
                containsInAnyOrder("SERVICES-UPDATE-1", "SERVICES-UPDATE-2")
            );
        }

        @Test
        @DisplayName("Should not update BuildingLocationCache after empty ReferenceData response")
        void shouldNotUpdateBuildingLocationCacheAfterEmptyReferenceDataResponse() {

            // GIVEN
            BuildingLocation[] expectedResponse = new BuildingLocation[]{};
            configMockRestTemplate(ReferenceDataRepository.BUILDING_LOCATIONS_PATH, expectedResponse);

            // WHEN
            underTest.updateBuildingLocationCache();

            // THEN
            verify(cacheManager, never()).getCache(BUILDING_LOCATIONS_CACHE);
            verify(cache, never()).putIfAbsent(eq(BUILDING_LOCATIONS_CACHE_KEY), any());
        }

        @Test
        @DisplayName("Should not update ServicesCache after empty ReferenceData response")
        void shouldNotUpdateServicesCacheAfterEmptyReferenceDataResponse() {

            // GIVEN
            ServiceReferenceData[] expectedResponse = new ServiceReferenceData[]{};
            configMockRestTemplate(ReferenceDataRepository.SERVICES_PATH, expectedResponse);

            // WHEN
            underTest.updateServicesCache();

            // THEN
            verify(cacheManager, never()).getCache(SERVICES_CACHE);
            verify(cache, never()).putIfAbsent(eq(SERVICES_CACHE_KEY), any());
        }

        private void configMockCache(String name) {
            when(cacheManager.getCache(name)).thenReturn(cache);
        }
    }

    private <T> void verifyApiCallSignature(String path, Class<T> clazz) {

        verify(restTemplate)
            .exchange(eq(getRefDataUrl(path)), eq(HttpMethod.GET), entityCaptor.capture(), (Class<?>)eq(clazz));

        // verify correct 'SystemUser' headers have been used
        assertEquals(systemUserHttpHeaders, entityCaptor.getValue().getHeaders());
    }

    @SuppressWarnings("unchecked")
    private <T> void configMockRestTemplate(String path, T[] response) {
        ResponseEntity<T[]> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate
            .exchange(eq(getRefDataUrl(path)), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class))
        )
            .thenReturn(responseEntity);
    }

    @SuppressWarnings("unchecked")
    private void configMockRestTemplateThrowException(String path) {
        when(restTemplate
            .exchange(eq(getRefDataUrl(path)), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class))
        )
            .thenThrow(new RestClientException("test"));
    }

    private String getRefDataUrl(String path) {
        return TEST_REF_DATA_URL + path;
    }
}
