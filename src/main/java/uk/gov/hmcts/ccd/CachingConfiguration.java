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

    public static final String HAZELCAST_HEALTH_MONITORING_LEVEL = "hazelcast.health.monitoring.level";
    public static final String HAZELCAST_HEALTH_MONITORING_DELAY_SECONDS = "hazelcast.health.monitoring.delay.seconds";
    public static final String HAZELCAST_JMX = "hazelcast.jmx";

    @Autowired
    ApplicationParams applicationParams;

    @Bean
    public Config hazelCastConfig() {

        Config config = new Config();
        NetworkConfig networkConfig = config.setInstanceName("hazelcast-instance-ccd").getNetworkConfig();
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
        configCaches(applicationParams.getDefinitionCacheMaxIdleSecs(), applicationParams.getLatestVersionTTLSecs(), config);

        config.setProperty(HAZELCAST_HEALTH_MONITORING_LEVEL, applicationParams.getHazelcastHealthMonitoringLevel());
        config.setProperty(HAZELCAST_HEALTH_MONITORING_DELAY_SECONDS, applicationParams.getHazelcastHealthMonitoringDelaySeconds());
        config.setProperty(HAZELCAST_JMX, applicationParams.getHazelcastJmx());
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
