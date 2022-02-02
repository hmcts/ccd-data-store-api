package uk.gov.hmcts.ccd;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@EnableCaching
@EnableScheduling
@Configuration
public class RedisCachingConfiguration {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ApplicationParams applicationParams) {
        final int definitionCacheMaxIdle = applicationParams.getDefinitionCacheMaxIdleSecs();
        final int latestVersionTTL = applicationParams.getLatestVersionTTLSecs();

        return (builder) -> builder
                .withCacheConfiguration("caseTypeDefinitionsCache",
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                            Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("workBasketResultCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("searchResultCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("searchCasesResultCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("searchInputDefinitionCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("workbasketInputDefinitionCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("caseTabCollectionCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("wizardPageCollectionCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("allJurisdictionsCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("userRolesCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("caseTypePseudoRoleToAccessProfileCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(definitionCacheMaxIdle)))
                .withCacheConfiguration("userInfoCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(applicationParams.getUserCacheTTLSecs())))
                .withCacheConfiguration("damUserRoleCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(applicationParams.getUserCacheTTLSecs())))
                .withCacheConfiguration("bannersCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
                                Duration.ofSeconds(latestVersionTTL)));
//                .withCacheConfiguration("jurisdictionUiConfigsCache",
//                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
//                                Duration.ofSeconds(latestVersionTTL)))
//                .withCacheConfiguration("caseTypeDefinitionLatestVersionCache",
//                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
//                                Duration.ofSeconds(latestVersionTTL)))
//                .withCacheConfiguration("jurisdictionCache",
//                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
//                                Duration.ofSeconds(applicationParams.getJurisdictionTTLSecs())))
//                .withCacheConfiguration("BUILDING_LOCATIONS_CACHE",
//                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
//                                Duration.ofSeconds(applicationParams.getRefDataCacheTtlInSec())))
//                .withCacheConfiguration("SERVICES_CACHE",
//                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(
//                                Duration.ofSeconds(applicationParams.getRefDataCacheTtlInSec())));
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

}
