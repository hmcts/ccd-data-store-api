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
        final int definitionCacheMaxIdle = applicationParams.getDefinitionCacheMaxIdleSecs();
        final int latestVersionTTL = applicationParams.getLatestVersionTTLSecs();
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workBasketResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchCasesResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workbasketInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTabCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("wizardPageCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("allJurisdictionsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userRolesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypePseudoRoleToAccessProfileCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userInfoCache", applicationParams.getUserCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithMaxIdle("idamUserRoleCache",
            applicationParams.getUserCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithMaxIdle("bannersCache", latestVersionTTL));
        config.addMapConfig(newMapConfigWithMaxIdle("jurisdictionUiConfigsCache", latestVersionTTL));
        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", latestVersionTTL));
        config.addMapConfig(newMapConfigWithTtl("caseRolesCache", latestVersionTTL));
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
                .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(applicationParams.getDefinitionCacheMaxSize());
        return new MapConfig().setName(name)
                .setEvictionConfig(evictionConfig);
    }

}
