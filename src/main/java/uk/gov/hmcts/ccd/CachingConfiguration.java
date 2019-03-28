package uk.gov.hmcts.ccd;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.DiscoveryConfig;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryStrategyFactory;
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
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(false);
        if (applicationParams
                .isHazelcastDiscoveryEnabled()) {
            configConsulDiscoveryStrategy(config, joinConfig);
        }
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
    }

    private MapConfig newMapConfig(final String name, int definitionCacheTTL) {
        return new MapConfig().setName(name)
                .setMaxSizeConfig(new MaxSizeConfig(applicationParams.getDefinitionCacheMaxSize(), MaxSizeConfig.MaxSizePolicy.PER_NODE))
                .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
                .setMaxIdleSeconds(definitionCacheTTL);
    }

    private void configConsulDiscoveryStrategy(Config config, JoinConfig joinConfig) {
        config.setProperty("hazelcast.discovery.enabled", String.valueOf(applicationParams
                                                                                 .isHazelcastDiscoveryEnabled()));
        final DiscoveryConfig discoveryConfig = joinConfig.getDiscoveryConfig();
        DiscoveryStrategyFactory discoveryStrategyFactory = new ConsulDiscoveryStrategyFactory();
        DiscoveryStrategyConfig strategyConfig = new DiscoveryStrategyConfig(discoveryStrategyFactory);
        strategyConfig.addProperty("consul-host", applicationParams.getHazelcastDiscoveryConsulHost());
        strategyConfig.addProperty("consul-port", applicationParams.getHazelcastDiscoveryConsulPort());
        strategyConfig.addProperty("consul-service-name", "hz-discovery-cluster");
        strategyConfig.addProperty("consul-healthy-only", applicationParams.isHazelcastDiscoveryConsulHealthyOnly());
        strategyConfig.addProperty("consul-discovery-delay-ms", String.valueOf(applicationParams
                                                                                       .getHazelcastDiscoveryConsulDelayInMs()));
        strategyConfig.addProperty("consul-registrator", applicationParams.getHazelcastDiscoveryConsulRegistrator());
        discoveryConfig.addDiscoveryStrategyConfig(strategyConfig);
    }

}
