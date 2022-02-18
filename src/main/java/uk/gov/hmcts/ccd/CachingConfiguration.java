package uk.gov.hmcts.ccd;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.NetworkConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE;

@Configuration
@EnableCaching
@EnableScheduling
public class CachingConfiguration {

    @Autowired
    ApplicationParams applicationParams;

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
        final int defaultCacheMaxIdle = applicationParams.getDefaultCacheMaxIdleSecs();
        final int defaultCacheTtl = applicationParams.getDefaultCacheTtlSecs();
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionsCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workBasketResultCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchResultCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchCasesResultCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchInputDefinitionCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workbasketInputDefinitionCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTabCollectionCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("wizardPageCollectionCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("allJurisdictionsCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userRolesCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypePseudoRoleToAccessProfileCache", defaultCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userInfoCache", applicationParams.getUserCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithMaxIdle("idamUserRoleCache",
            applicationParams.getUserCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithTtl("systemUserTokenCache",
            applicationParams.getSystemUserTokenCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithMaxIdle("bannersCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithMaxIdle("jurisdictionUiConfigsCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionCache", applicationParams.getJurisdictionTTLSecs()));
        config.addMapConfig(newMapConfigWithTtl(BUILDING_LOCATIONS_CACHE, applicationParams.getRefDataCacheTtlInSec()));
        config.addMapConfig(newMapConfigWithTtl(SERVICES_CACHE, applicationParams.getRefDataCacheTtlInSec()));
    }

    private MapConfig newMapConfigWithMaxIdle(final String name, final Integer maxIdle) {
        return newMapConfig(name).setMaxIdleSeconds(maxIdle);
    }

    private MapConfig newMapConfigWithTtl(final String name, final Integer ttl) {
        return newMapConfig(name).setTimeToLiveSeconds(ttl);
    }

    private MapConfig newMapConfig(final String name) {
        final EvictionConfig evictionConfig = new EvictionConfig()
                .setEvictionPolicy(applicationParams.getDefaultCacheEvictionPolicy())
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(applicationParams.getDefaultCacheMaxSize());
        return new MapConfig().setName(name)
                .setEvictionConfig(evictionConfig);
    }

}
