package uk.gov.hmcts.ccd;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        configCaches(applicationParams.getDefinitionCacheMaxIdleSecs(), applicationParams.getLatestVersionTTLSecs(), config);
        return config;
    }

    private void configCaches(int definitionCacheMaxIdle, int latestVersionTTL, Config config) {
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workBasketResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workbasketInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTabCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("wizardPageCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userRolesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userCache", applicationParams.getUserCacheTTLSecs()));
        config.addMapConfig(newMapConfigWithTtl("caseTypeDefinitionLatestVersionCache", latestVersionTTL));
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
