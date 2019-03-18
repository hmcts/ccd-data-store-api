package uk.gov.hmcts.ccd;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;

import java.util.Optional;

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
        NetworkConfig networkConfig = config.setInstanceName("hazelcast-instance-ccd").getNetworkConfig();
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
        configCaches(applicationParams.getDefinitionCacheMaxIdleSecs(), applicationParams.getDefinitionCacheTTLSecs(), config);
        return config;
    }

    private void configCaches(int definitionCacheMaxIdle, int definitionCacheTTL, Config config) {
        config.addMapConfig(newMapConfig("caseTypeDefinitionsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("workBasketResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("searchResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("searchInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("workbasketInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("caseTabCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("wizardPageCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("userRolesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfig("caseTypeDefinitionLatestVersionCache", definitionCacheMaxIdle, definitionCacheTTL));
        
    }

    private MapConfig newMapConfig(final String name, Integer definitionCacheMaxIdle) {
        return newMapConfig(name, definitionCacheMaxIdle, null);
    }

    private MapConfig newMapConfig(final String name, Integer definitionCacheMaxIdle, Integer definitionCacheTTL) {
        MapConfig mapConfig = new MapConfig().setName(name)
                .setMaxSizeConfig(new MaxSizeConfig(applicationParams.getDefinitionCacheMaxSize(), MaxSizeConfig.MaxSizePolicy.PER_NODE))
                .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
                .setMaxIdleSeconds(definitionCacheMaxIdle);
        Optional.ofNullable(definitionCacheTTL).ifPresent(value -> mapConfig.setTimeToLiveSeconds(value));
        return mapConfig;
    }

}
