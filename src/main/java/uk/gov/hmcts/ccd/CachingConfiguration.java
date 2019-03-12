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

    private final ApplicationParams applicationParams;

    @Autowired
    public CachingConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public Config hazelCastConfig() {

        Config config = new Config();
        NetworkConfig networkConfig = config.setInstanceName("hazelcast-instance-ccd").getNetworkConfig();
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
        configCaches(applicationParams.getDefinitionCacheTTLSecs(), config);
        return config;
    }

    private void configCaches(int definitionCacheTTL, Config config) {
        config.addMapConfig(newMapConfig("caseTypeDefinitionsCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("workBasketResultCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("searchResultCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("searchInputDefinitionCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("workbasketInputDefinitionCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("caseTabCollectionCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("wizardPageCollectionCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("userRolesCache", definitionCacheTTL));
        config.addMapConfig(newMapConfig("userCache", applicationParams.getDailyCacheTTLSecs()));
    }

    private MapConfig newMapConfig(final String name, int definitionCacheTTL) {
        return new MapConfig().setName(name)
                .setMaxSizeConfig(new MaxSizeConfig(applicationParams.getDefinitionCacheMaxSize(), MaxSizeConfig.MaxSizePolicy.PER_NODE))
                .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
                .setMaxIdleSeconds(definitionCacheTTL);
    }

}
