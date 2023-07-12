package uk.gov.hmcts.ccd.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.time.Duration;
import java.util.List;

import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfiguration {

    private static final int TTL_ZERO = 0;
    private final ApplicationParams applicationParams;

    public CacheConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public CacheManager cacheManagerTicker() {
        final int defaultMaxIdle = applicationParams.getDefaultCacheMaxIdleSecs();
        final int defaultCacheTtl = applicationParams.getDefaultCacheTtlSecs();
        final int userCacheTtl = applicationParams.getUserCacheTTLSecs();
        final int jurisdictionCacheTtl = applicationParams.getJurisdictionTTLSecs();
        final int systemUserTokenCacheTTLSecs = applicationParams.getSystemUserTokenCacheTTLSecs();

        var cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            newMapConfigWithMaxIdle("caseTypeDefinitionsCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("workBasketResultCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("searchResultCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("searchCasesResultCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("searchInputDefinitionCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("workbasketInputDefinitionCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("caseTabCollectionCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("wizardPageCollectionCache", defaultMaxIdle),
            newMapConfigWithMaxIdle("caseTypePseudoRoleToAccessProfileCache", defaultMaxIdle),

            newMapConfigWithTtl("allJurisdictionsCache", jurisdictionCacheTtl),
            newMapConfigWithTtl("userRolesCache", userCacheTtl),
            newMapConfigWithTtl("userInfoCache", userCacheTtl), //40
            newMapConfigWithTtl("idamUserRoleCache", userCacheTtl),
            newMapConfigWithTtl("systemUserTokenCache", systemUserTokenCacheTTLSecs),
            newMapConfigWithTtl("bannersCache", defaultCacheTtl),
            newMapConfigWithTtl("jurisdictionUiConfigsCache", defaultCacheTtl), //30
            newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", defaultCacheTtl),
            newMapConfigWithTtl("caseRolesCache", defaultCacheTtl),
            newMapConfigWithTtl("jurisdictionCache", jurisdictionCacheTtl),
            newMapConfigWithTtl(BUILDING_LOCATIONS_CACHE, applicationParams.getRefDataCacheTtlInSec()),
            newMapConfigWithTtl(SERVICES_CACHE, applicationParams.getRefDataCacheTtlInSec())
        ));

        return cacheManager;
    }

    private CaffeineCache newMapConfigWithMaxIdle(final String cacheName, final Integer maxIdle) {
        return buildCache(cacheName, TTL_ZERO, maxIdle);
    }

    private CaffeineCache newMapConfigWithTtl(final String cacheName, final Integer ttl) {
        return buildCache(cacheName, ttl, applicationParams.getDefaultCacheMaxIdleSecs());
    }

    private CaffeineCache buildCache(String cacheName, int ttl, int maxIdle) {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        if (ttl > 0) {
            cacheBuilder.expireAfterWrite(Duration.ofSeconds(ttl));
        }

        if (maxIdle > 0) {
            cacheBuilder.expireAfterAccess(Duration.ofSeconds(maxIdle));
        }

        cacheBuilder.maximumSize(applicationParams.getDefaultCacheMaxSize());
        return new CaffeineCache(cacheName, cacheBuilder.build());
    }
}
