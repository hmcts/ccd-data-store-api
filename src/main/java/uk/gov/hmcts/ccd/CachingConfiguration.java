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
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("!test")
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
        configCachesForCaseDetailsRepository(config);
        configCachesForCaseDefinitionRepository(config);
        configCachesForUIDefinitionGateway(config);
        configCachesForDraftGateway(config);
        configCachesForUserRepository(config);
        configCachesForIdamRepository(config);
    }

    private void configCachesForCaseRoleRepository(Config config) {
        config.addMapConfig(newMapConfigWithTtl(
            "caseRolesForCaseTypeCache", applicationParams.getCaseDetailsCacheTTLSecs()));
    }

    private void configCachesForCaseUserRepository(Config config) {
        final int userCacheTTLSecs = applicationParams.getUserCacheTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("casesForUserCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("caseRolesForUserCache", userCacheTTLSecs));
    }

    private void configCachesForCaseDetailsRepository(Config config) {
        final int caseDetailsCacheTTLSecs = applicationParams.getCaseDetailsCacheTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("caseDetailsByJurisdictionAndIDCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl(
            "caseDetailsByJurisdictionAndReferenceCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("caseDetailsByReferenceCache", caseDetailsCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl(
            "caseDetailsByReferenceWithNoAccessCtlCache", caseDetailsCacheTTLSecs));
    }

    private void configCachesForCaseDefinitionRepository(Config config) {
        final int definitionCacheMaxIdle = applicationParams.getDefinitionCacheMaxIdleSecs();
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypesForJurisdictionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionsCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("userRolesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("caseTypeDefinitionLatestVersionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("jurisdictionCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("allCaseTypesCache", definitionCacheMaxIdle));
        config.addMapConfig(newMapConfigWithMaxIdle("baseTypesCache", definitionCacheMaxIdle));
    }

    private void configCachesForUIDefinitionGateway(Config config) {
        final int latestVersionCacheTTLSecs = applicationParams.getLatestVersionTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("workBasketResultCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("searchResultCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("searchCasesResultCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("searchInputDefinitionCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("workbasketInputDefinitionCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("caseTabCollectionCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("wizardPageCollectionCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("bannersCache", latestVersionCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("jurisdictionUiConfigsCache", latestVersionCacheTTLSecs));
    }

    private void configCachesForDraftGateway(Config config) {
        config.addMapConfig(newMapConfigWithTtl(
            "draftResponseCache", applicationParams.getDraftCacheTTLSecs()));
    }

    private void configCachesForUserRepository(Config config) {
        final int userCacheTTLSecs = applicationParams.getUserCacheTTLSecs();

        config.addMapConfig(newMapConfigWithTtl("userDetailsCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("userCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("userByTokenCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("userDefaultSettingsCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("userRolesCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("userClassificationsByJurisdictionCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("highestUserClassificationCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("userIDCache", userCacheTTLSecs));
        config.addMapConfig(newMapConfigWithTtl("caseworkerUserRolesJurisdictionsCache", userCacheTTLSecs));
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
        MapConfig mapConfig = new MapConfig().setName(name)
                .setEvictionConfig(evictionConfig);
        return mapConfig;
    }

}
