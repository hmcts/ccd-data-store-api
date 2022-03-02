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
        final int defaultCacheMaxIdle = appParams.getDefaultCacheMaxIdleSecs();
        final int defaultCacheTtl = appParams.getDefaultCacheTtlSecs();
        final int userCacheTtl = appParams.getUserCacheTTLSecs();
        final int jurisdictionCacheTtl = appParams.getJurisdictionTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionsCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("workBasketResultCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("searchResultCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("searchCasesResultCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("searchInputDefinitionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("workbasketInputDefinitionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("caseTabCollectionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("wizardPageCollectionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("allJurisdictionsCache", jurisdictionCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("userRolesCache", userCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("caseTypePseudoRoleToAccessProfileCache", userCacheTtl));

        config.addMapConfig(newMapConfigWithTtl("userInfoCache", userCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("idamUserRoleCache", userCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("systemUserTokenCache", appParams.getSystemUserTokenCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithTtl("bannersCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionUiConfigsCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", defaultCacheTtl));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionCache", jurisdictionCacheTtl));
        config.addMapConfig(newMapConfigWithTtl(BUILDING_LOCATIONS_CACHE, appParams.getRefDataCacheTtlInSec()));
        config.addMapConfig(newMapConfigWithTtl(SERVICES_CACHE, appParams.getRefDataCacheTtlInSec()));
    }

    private MapConfig newMapConfigWithMaxIdle(final String name, final Integer maxIdle) {
        final int defaultCacheTtl = appParams.getDefaultCacheTtlSecs();
        return newMapConfig(name).setMaxIdleSeconds(maxIdle).setTimeToLiveSeconds(defaultCacheTtl);
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
