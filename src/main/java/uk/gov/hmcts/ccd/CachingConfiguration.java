package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;

@Configuration
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
        config.addMapConfig(newMapConfigWithMaxIdle("userRolesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userInfoCache", applicationParams.getUserCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithMaxIdle("bannersCache", latestVersionTTL));
        config.addMapConfig(newMapConfigWithMaxIdle("jurisdictionUiConfigsCache", latestVersionTTL));
        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", latestVersionTTL));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionCache", applicationParams.getJurisdictionTTLSecs()));
    }

    private MapConfig newMapConfigWithMaxIdle(final String name, final Integer maxIdle) {
        return newMapConfig(name).setMaxIdleSeconds(maxIdle);
    }

    private MapConfig newMapConfigWithTtl(final String name, final Integer ttl) {
        return newMapConfig(name).setTimeToLiveSeconds(ttl);
    }

    private MapConfig newMapConfig(final String name) {
        MapConfig mapConfig = new MapConfig().setName(name)
                .setMaxSizeConfig(new MaxSizeConfig(applicationParams.getDefinitionCacheMaxSize(), MaxSizeConfig.MaxSizePolicy.PER_NODE))
                .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy());
        return mapConfig;
    }

}
