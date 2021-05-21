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

@Configuration
@EnableCaching
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
        configCachesForCaseRoleRepository(config);
        configCachesForCaseUserRepository(config);
        configCachesForCaseDetailsRepository(config);
        configCachesForCaseDefinitionRepository(config);
        configCachesForUIDefinitionGateway(config);
        configCachesForDraftGateway(config);
        configCachesForUserRepository(config);
        configCachesForIdamRepository(config);
    }

    private void configCachesForCaseRoleRepository(Config config) {
        config.addMapConfig(newMapConfigWithMaxIdle(
            "caseRolesForCaseTypeCache", applicationParams.getDefinitionCacheMaxIdleSecs()));
    }

    private void configCachesForCaseUserRepository(Config config) {
        final int userCacheTTLSecs = applicationParams.getUserCacheTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("casesForUserCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("caseRolesForUserCache", userCacheTTLSecs));
    }

    private void configCachesForCaseDetailsRepository(Config config) {
        final int caseDetailsCacheTTLSecs = applicationParams.getDefinitionCacheMaxIdleSecs();

        config.addMapConfig(newMapConfigWithMaxIdle("caseDetailsByIDCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithMaxIdle("caseDetailsByReferenceCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithMaxIdle("uniqueCaseDetailsCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithMaxIdle("paginatedSearchMetadataCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithMaxIdle(
                "caseDetailsByMetaDataAndFieldDataCache", caseDetailsCacheTTLSecs));
    }

    private void configCachesForCaseDefinitionRepository(Config config) {
        final int definitionCacheMaxIdle = applicationParams.getDefinitionCacheMaxIdleSecs();

        config.addMapConfig(newMapConfigWithMaxIdle("caseTypesForJurisdictionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userRoleClassificationsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithTtl(
            "jurisdictionCache", applicationParams.getJurisdictionTTLSecs()));
        config.addMapConfig(newMapConfigWithMaxIdle("baseTypesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithTtl(
            "caseTypeDefinitionLatestVersionCache", applicationParams.getLatestVersionTTLSecs()));
    }

    private void configCachesForUIDefinitionGateway(Config config) {
        final int latestVersionCacheTTLSecs = applicationParams.getLatestVersionTTLSecs();
        final int definitionCacheMaxIdle = applicationParams.getDefinitionCacheMaxIdleSecs();

        config.addMapConfig(newMapConfigWithMaxIdle("workBasketResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchCasesResultCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("searchInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("workbasketInputDefinitionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTabCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("wizardPageCollectionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithTtl("bannersCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionUiConfigsCache", latestVersionCacheTTLSecs));
    }

    private void configCachesForDraftGateway(Config config) {
        final int draftCacheTTLSecs = applicationParams.getDefinitionCacheMaxIdleSecs();

        config.addMapConfig(newMapConfigWithMaxIdle("draftResponseCache", draftCacheTTLSecs));
        config.addMapConfig(newMapConfigWithMaxIdle("draftResponseCaseDetailsCache", draftCacheTTLSecs));
    }

    private void configCachesForUserRepository(Config config) {
        final int userCacheTTLSecs = applicationParams.getUserCacheTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("userClassificationsByJurisdictionCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("highestUserClassificationCache", userCacheTTLSecs));
    }

    private void configCachesForIdamRepository(Config config) {
        config.addMapConfig(newMapConfigWithTtl("userInfoCache", applicationParams.getUserCacheTTLSecs()));
    }

    private MapConfig newMapConfigWithMaxIdle(final String name, final Integer maxIdle) {
        return newMapConfig(name).setMaxIdleSeconds(maxIdle);
    }

    private MapConfig newMapConfigWithTtl(final String name, final Integer ttl) {
        return newMapConfig(name).setTimeToLiveSeconds(ttl);
    }

    private MapConfig newMapConfig(final String name) {
        EvictionConfig evictionConfig = new EvictionConfig()
                .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(applicationParams.getDefinitionCacheMaxSize());

        return new MapConfig().setName(name)
                .setEvictionConfig(evictionConfig);
    }

}
