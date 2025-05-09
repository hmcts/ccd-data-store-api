package uk.gov.hmcts.ccd.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Named
@Slf4j
@SuppressWarnings("squid:S1075") // paths below are not URI path literals
public class ReferenceDataRepository {

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final CacheManager cacheManager;

    private static final String RESULT_IS_NULL_OR_EMPTY = "#result==null or #result.isEmpty()";

    public static final String BUILDING_LOCATIONS_CACHE = "buildingLocations";
    public static final String BUILDING_LOCATIONS_CACHE_KEY = "getBuildingLocations";
    public static final String SERVICES_CACHE = "orgServices";
    public static final String SERVICES_CACHE_KEY = "getServices";

    public static final String BUILDING_LOCATIONS_PATH = "/refdata/location/building-locations";
    public static final String SERVICES_PATH = "/refdata/location/orgServices";

    @Inject
    public ReferenceDataRepository(final SecurityUtils securityUtils,
                                   @Qualifier("restTemplate") final RestTemplate restTemplate,
                                   final ApplicationParams applicationParams,
                                   final CacheManager cacheManager) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.cacheManager = cacheManager;
    }

    @Cacheable(
        cacheNames = BUILDING_LOCATIONS_CACHE,
        key = "T(uk.gov.hmcts.ccd.data.ReferenceDataRepository).BUILDING_LOCATIONS_CACHE_KEY",
        unless = RESULT_IS_NULL_OR_EMPTY
    )
    public List<BuildingLocation> getBuildingLocations() {
        return getReferenceData(BUILDING_LOCATIONS_PATH, BuildingLocation[].class);
    }

    @Cacheable(
        cacheNames = SERVICES_CACHE,
        key = "T(uk.gov.hmcts.ccd.data.ReferenceDataRepository).SERVICES_CACHE_KEY",
        unless = RESULT_IS_NULL_OR_EMPTY
    )
    public List<ServiceReferenceData> getServices() {
        return getReferenceData(SERVICES_PATH, ServiceReferenceData[].class);
    }

    private <T> List<T> getReferenceData(final String path, final Class<T[]> responseType) {
        try {

            log.debug("getReferenceData: " + path);

            final T[] result = restTemplate.exchange(
                    applicationParams.getReferenceDataApiUrl() + path,
                    HttpMethod.GET,
                    new HttpEntity<>(securityUtils.authorizationHeadersForDataStoreSystemUser()),
                    responseType)
                .getBody();

            return Optional.ofNullable(result)
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        } catch (RestClientException e) {
            log.error("Error fetching reference data: ", e);
            return Collections.emptyList();
        }
    }

    @Scheduled(cron = "${reference.data.cache.refresh.rate.cron}")
    public void updateBuildingLocationCache() {
        final List<BuildingLocation> buildingLocations = getBuildingLocations();
        updateCache(BUILDING_LOCATIONS_CACHE, BUILDING_LOCATIONS_CACHE_KEY, buildingLocations);
        log.info("Scheduled update of {} cache with {} records.", BUILDING_LOCATIONS_CACHE, buildingLocations.size());
    }

    @Scheduled(cron = "${reference.data.cache.refresh.rate.cron}")
    public void updateServicesCache() {
        final List<ServiceReferenceData> services = getServices();
        updateCache(SERVICES_CACHE, SERVICES_CACHE_KEY, services);
        log.info("Scheduled update of {} cache with {} records.", SERVICES_CACHE, services.size());
    }

    private <T> void updateCache(final String cacheName, final String cacheKey, final List<T> newValue) {
        if (!newValue.isEmpty()) {
            invalidateCache(cacheName);
            putCache(cacheName, cacheKey, newValue);
            log.debug("Update " + cacheName + " cache with " + newValue.size() + " records.");
        }
    }

    void invalidateCache(final String cacheName) {
        Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(Cache::invalidate);
    }

    private <T> void putCache(final String cacheName, final String cacheKey, final List<T> newValue) {
        Optional.ofNullable(cacheManager.getCache(cacheName))
            .ifPresent(cache -> cache.putIfAbsent(cacheKey, newValue));
    }
}
