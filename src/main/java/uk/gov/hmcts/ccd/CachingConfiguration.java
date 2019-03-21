package uk.gov.hmcts.ccd;

import com.google.common.collect.Lists;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.idam.IdamHelper;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CachingConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CachingConfiguration.class);

    @Autowired
    private final ApplicationParams applicationParams;
    @Qualifier("restTemplate")
    @Autowired
    private final RestTemplate restTemplate;
    private final IdamHelper idamHelper;
    private final AuthTokenGenerator authTokenGenerator;

    @Inject
    public CachingConfiguration(final ApplicationParams applicationParams,
                                final RestTemplate restTemplate,
                                final IdamHelper idamHelper,
                                final AuthTokenGenerator authTokenGenerator) {
        this.applicationParams = applicationParams;
        this.restTemplate = restTemplate;
        this.idamHelper = idamHelper;
        this.authTokenGenerator = authTokenGenerator;
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

    @Bean
    public HazelcastInstance hazelCastInstance() {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelCastConfig());
        warmUp(hazelcastInstance);
        return hazelcastInstance;
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

    private void warmUp(final HazelcastInstance hazelcastInstance) {
        IMap<String, CaseType> caseTypeDefinitionsCache = hazelcastInstance.getMap("caseTypeDefinitionsCache");
        List<CaseType> allCaseTypes = getAllCaseTypes();
        allCaseTypes.stream().forEach(caseType -> {
            caseTypeDefinitionsCache.put(caseType.getId(), caseType);
        });
    }

    private MapConfig newMapConfig(final String name, int definitionCacheTTL) {
        return new MapConfig().setName(name)
            .setMaxSizeConfig(new MaxSizeConfig(applicationParams.getDefinitionCacheMaxSize(), MaxSizeConfig.MaxSizePolicy.PER_NODE))
            .setEvictionPolicy(applicationParams.getDefinitionCacheEvictionPolicy())
            .setMaxIdleSeconds(definitionCacheTTL);
    }

    private List<CaseType> getAllCaseTypes() {
        LOG.debug("retrieving all case types definitions to warm up caseTypeDefinitionsCache");
        try {
            final HttpEntity requestEntity = new HttpEntity<CaseType>(authorizationHeaders());
            return Arrays.asList(restTemplate.exchange(applicationParams.caseTypesDefURL(), HttpMethod.GET, requestEntity, CaseType[].class).getBody());

        } catch (Exception e) {
            LOG.warn("Error while retrieving all case types to warm up caseTypeDefinitionsCache", e);
            return Lists.newArrayList();
        }
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", authTokenGenerator.generate());
        headers.add(HttpHeaders.AUTHORIZATION, getUserToken());
        return headers;
    }

    private String getUserToken() {
        AuthenticatedUser caseworker = idamHelper.authenticate(applicationParams.getCacheWarmUpEmail(), applicationParams.getCacheWarmUpPassword());
        return caseworker.getAccessToken();
    }

}
