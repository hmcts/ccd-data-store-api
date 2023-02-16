package uk.gov.hmcts.ccd;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.NetworkConfig;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.redis.RedisHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.redis.RedisReactiveHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE;

@Configuration
@ConditionalOnProperty(
    name = "spring.cache.type",
    havingValue = "hazelcast"
)
//Exclude autoconfiguration via application.yml or
@SpringBootApplication(
    exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        RedisReactiveHealthContributorAutoConfiguration.class,
        RedisHealthContributorAutoConfiguration.class,
        RedissonAutoConfiguration.class
    }
)
@EnableCaching
@EnableScheduling
public class HazelcastCachingConfiguration {

    @Autowired
    ApplicationParams appParams;

    @Bean
    public Config hazelCastConfig() {

        Config config = new Config();
        config.setProperty("hazelcast.phone.home.enabled", "false");
        NetworkConfig networkConfig = config.setInstanceName("hazelcast-instance-ccd").getNetworkConfig();
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
        configCaches(config);
        return config;
    }

    private void configCaches(Config config) {
        final int defaultMaxIdle = appParams.getDefaultCacheMaxIdleSecs();
        final int defaultCacheTtl = appParams.getDefaultCacheTtlSecs();
        final int userCacheTtl = appParams.getUserCacheTTLSecs();
        final int jurisdictionCacheTtl = appParams.getJurisdictionTTLSecs();
        final int systemUserTokenCacheTTLSecs = appParams.getSystemUserTokenCacheTTLSecs();

        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionsCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workBasketResultCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchResultCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchCasesResultCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchInputDefinitionCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workbasketInputDefinitionCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTabCollectionCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("wizardPageCollectionCache", defaultMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypePseudoRoleToAccessProfileCache", defaultMaxIdle));

        config.addMapConfig(newMapConfigWithTtl("allJurisdictionsCache", jurisdictionCacheTtl)); // Fixed.
        config.addMapConfig(newMapConfigWithTtl("userRolesCache", userCacheTtl)); // Fixed.
        config.addMapConfig(newMapConfigWithTtl("userInfoCache", userCacheTtl)); // Fixed.
        config.addMapConfig(newMapConfigWithTtl("idamUserRoleCache", userCacheTtl)); // Fixed.
        config.addMapConfig(newMapConfigWithTtl("systemUserTokenCache", systemUserTokenCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("bannersCache", defaultCacheTtl)); // Fixed.
        config.addMapConfig(newMapConfigWithTtl("jurisdictionUiConfigsCache", defaultCacheTtl)); // Fixed.
        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("caseRolesCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionCache", jurisdictionCacheTtl));
        config.addMapConfig(newMapConfigWithTtl(BUILDING_LOCATIONS_CACHE, appParams.getRefDataCacheTtlInSec())
            .setMaxIdleSeconds(0));
        config.addMapConfig(newMapConfigWithTtl(SERVICES_CACHE, appParams.getRefDataCacheTtlInSec())
            .setMaxIdleSeconds(0));
    }

    private MapConfig newMapConfigWithMaxIdle(final String name, final Integer maxIdle) {
        return newMapConfig(name).setMaxIdleSeconds(maxIdle);
    }

    private MapConfig newMapConfigWithTtl(final String name, final Integer ttl) {
        final int defaultCacheMaxIdle = appParams.getDefaultCacheMaxIdleSecs();
        return newMapConfig(name).setTimeToLiveSeconds(ttl).setMaxIdleSeconds(defaultCacheMaxIdle);
    }

    private MapConfig newMapConfig(final String name) {
        final EvictionConfig evictionConfig = new EvictionConfig()
            .setEvictionPolicy(appParams.getDefaultCacheEvictionPolicy())
            .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
            .setSize(appParams.getDefaultCacheMaxSize());
        return new MapConfig().setName(name)
            .setEvictionConfig(evictionConfig);
    }

}
