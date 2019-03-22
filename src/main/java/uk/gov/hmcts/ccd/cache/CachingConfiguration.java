package uk.gov.hmcts.ccd.cache;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import uk.gov.hmcts.ccd.ApplicationParams;

import javax.inject.Inject;

@Configuration
@EnableAsync
public class CachingConfiguration {

    private final ApplicationParams applicationParams;
    private final CacheWarmUpService cacheWarmUpService;

    @Inject
    public CachingConfiguration(final ApplicationParams applicationParams,
                                @Qualifier(DefaultCacheWarmUpService.QUALIFIER) final CacheWarmUpService cacheWarmUpService) {
        this.applicationParams = applicationParams;
        this.cacheWarmUpService = cacheWarmUpService;
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
        if (applicationParams.isCacheWarmUpEnabled()) {
            cacheWarmUpService.warmUp();
        }
    }

    private MapConfig newMapConfig(final String name, int definitionCacheTTL) {
        return new MapConfig().setName(name)
            .setMaxSizeConfig(new MaxSizeConfig(applicationParams.getDefinitionCacheMaxSize(), MaxSizeConfig.MaxSizePolicy.PER_NODE))
            .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
            .setMaxIdleSeconds(definitionCacheTTL);
    }

}
