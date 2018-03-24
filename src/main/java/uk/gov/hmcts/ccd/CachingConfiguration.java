package uk.gov.hmcts.ccd;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CachingConfiguration {

    @Bean
    public Config hazelCastConfig(){
        Config config = new Config();
        NetworkConfig networkConfig = config.setInstanceName("hazelcast-instance").getNetworkConfig();
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
        return config.addMapConfig(new MapConfig()
                                .setName("caseTypeDefinitions")
                                .setMaxSizeConfig(new MaxSizeConfig(200, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                .setTimeToLiveSeconds(2000))
                                .setProperty("hazelcast.multicast.enabled", "false")
                                .setProperty("hazelcast.tcp-ip.enabled", "false")
                                .setProperty("hazelcast.logging.type","slf4j");

    }

}