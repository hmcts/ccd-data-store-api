package uk.gov.hmcts.ccd;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE;

@Configuration
@ConditionalOnProperty(
    name = "spring.cache.type",
    havingValue = "redis"
)
@EnableCaching
@EnableScheduling
public class RedisCachingConfiguration {

    @Autowired
    private ApplicationParams appParams;

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        int defaultMaxIdle = appParams.getDefaultCacheMaxIdleSecs();
        int defaultCacheTtl = appParams.getDefaultCacheTtlSecs();
        int userCacheTtl = appParams.getUserCacheTTLSecs();
        int jurisdictionCacheTtl = appParams.getJurisdictionTTLSecs();
        int systemUserTokenCacheTTLSecs = appParams.getSystemUserTokenCacheTTLSecs();
        Map<String, CacheConfig> config = new HashMap<>();

        config.put("caseTypeDefinitionsCache", maxIdleConfig(defaultMaxIdle));
        config.put("workBasketResultCache", maxIdleConfig(defaultMaxIdle));
        config.put("searchResultCache", maxIdleConfig(defaultMaxIdle));
        config.put("searchCasesResultCache", maxIdleConfig(defaultMaxIdle));
        config.put("searchInputDefinitionCache", maxIdleConfig(defaultMaxIdle));
        config.put("workbasketInputDefinitionCache", maxIdleConfig(defaultMaxIdle));
        config.put("caseTabCollectionCache", maxIdleConfig(defaultMaxIdle));
        config.put("wizardPageCollectionCache", maxIdleConfig(defaultMaxIdle));
        config.put("caseTypePseudoRoleToAccessProfileCache", maxIdleConfig(defaultMaxIdle));

        config.put("allJurisdictionsCache", ttlConfig(jurisdictionCacheTtl));
        config.put("userRolesCache", ttlConfig(userCacheTtl));
        config.put("userInfoCache", ttlConfig(userCacheTtl));
        config.put("idamUserRoleCache", ttlConfig(userCacheTtl));
        config.put("systemUserTokenCache", ttlConfig(systemUserTokenCacheTTLSecs));
        config.put("bannersCache", ttlConfig(defaultCacheTtl));
        config.put("jurisdictionUiConfigsCache", ttlConfig(defaultCacheTtl));
        config.put("caseTypeDefinitionLatestVersionCache", ttlConfig(defaultCacheTtl));
        config.put("caseRolesCache", ttlConfig(defaultCacheTtl));
        config.put("jurisdictionCache", ttlConfig(jurisdictionCacheTtl));
        config.put(BUILDING_LOCATIONS_CACHE, new CacheConfig(appParams.getRefDataCacheTtlInSec(), 0));
        config.put(SERVICES_CACHE, new CacheConfig(appParams.getRefDataCacheTtlInSec(), 0));

        return new RedissonSpringCacheManager(redissonClient, config);
    }

    private CacheConfig maxIdleConfig(Integer maxIdle) {
        return new CacheConfig(0, maxIdle * 1000);
    }

    private CacheConfig ttlConfig(Integer ttl) {
        long defaultCacheMaxIdle = appParams.getDefaultCacheMaxIdleSecs();
        return new CacheConfig(ttl * 1000, defaultCacheMaxIdle * 1000);
    }
}
