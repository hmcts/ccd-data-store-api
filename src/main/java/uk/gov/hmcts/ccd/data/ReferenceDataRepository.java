package uk.gov.hmcts.ccd.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Named
@Slf4j
public class ReferenceDataRepository {
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final CacheManager cacheManager;

    private static final String BUILDING_LOCATIONS_CACHE = "buildingLocations";
    private static final String SERVICES_CACHE = "orgServices";
    private static final String CACHE_KEY = "#root.method.name";

    static final String BUILDING_LOCATIONS_PATH = "/refdata/location/building-locations";
    static final String SERVICES_PATH = "/refdata/location/orgServices";

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

    @Cacheable(cacheNames = BUILDING_LOCATIONS_CACHE, key = CACHE_KEY)
    public List<BuildingLocation> getBuildingLocations() {
        return getReferenceData(BUILDING_LOCATIONS_PATH, BuildingLocation[].class);
    }

    @Cacheable(cacheNames = SERVICES_CACHE, key = CACHE_KEY)
    public List<Service> getServices() {
        return getReferenceData(SERVICES_PATH, Service[].class);
    }

    private <T> List<T> getReferenceData(final String path, final Class<T[]> responseType) {
        try {
            final T[] result = restTemplate.exchange(
                    applicationParams.getReferenceDataApiUrl() + path,
                    HttpMethod.GET,
                    new HttpEntity<>(securityUtils.authorizationHeaders()),
                    responseType)
                .getBody();

            return Optional.ofNullable(result)
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        } catch (HttpClientErrorException e) {
            log.error("Error fetching reference data: ", e);
            return Collections.emptyList();
        }
    }

    @Scheduled(cron = "${reference.data.cache.refresh.rate.cron}")
    public void updateBuildingLocationCache() {
        final List<BuildingLocation> buildingLocations = getBuildingLocations();
        updateCache(BUILDING_LOCATIONS_CACHE, buildingLocations);
    }

    @Scheduled(cron = "${reference.data.cache.refresh.rate.cron}")
    public void updateServicesCache() {
        final List<Service> services = getServices();
        updateCache(SERVICES_CACHE, services);
    }

    private <T> void updateCache(final String cacheName, final List<T> newValue) {
        if (!newValue.isEmpty()) {
            clearCache(cacheName);
            putCache(cacheName, newValue);
        }
    }

    void clearCache(final String cacheName) {
        Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(Cache::invalidate);
    }

    private <T> void putCache(final String cacheName, final List<T> newValue) {
        Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(cache -> cache.put(CACHE_KEY, newValue));
    }
}
