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
        if (applicationParams.isCacheWarmUpEnabled()) {
            cacheWarmUpService.warmUp();
        }
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
