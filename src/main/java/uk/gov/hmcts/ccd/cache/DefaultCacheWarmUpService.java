package uk.gov.hmcts.ccd.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.idam.IdamHelper;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Qualifier(DefaultCacheWarmUpService.QUALIFIER)
public class DefaultCacheWarmUpService implements CacheWarmUpService {

    private static final Logger LOG = LoggerFactory.getLogger(CachingConfiguration.class);
    public static final String QUALIFIER = "default";

    private final DefaultCaseDefinitionRepository caseDefinitionRepository;
    private final AuthTokenGenerator authTokenGenerator;
    private final ApplicationParams applicationParams;
    private final IdamHelper idamHelper;

    public DefaultCacheWarmUpService(final DefaultCaseDefinitionRepository caseDefinitionRepository,
                                     final AuthTokenGenerator authTokenGenerator,
                                     final ApplicationParams applicationParams,
                                     final IdamHelper idamHelper) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.authTokenGenerator = authTokenGenerator;
        this.applicationParams = applicationParams;
        this.idamHelper = idamHelper;
    }

    @Async
    @Override
    public void warmUp() {
        try {
            HttpHeaders httpHeaders = authorizationHeaders();
            List<String> caseTypesReferences = caseDefinitionRepository.getCaseTypesReferences(httpHeaders);
            for (String reference : caseTypesReferences) {
                TimeUnit.MILLISECONDS.sleep(applicationParams.getCacheWarmUpSleepTime());
                try {
                    caseDefinitionRepository.getCaseType(reference, httpHeaders);
                } catch (Exception e) {
                    LOG.warn(String.format("Error while retrieving case type %s to warm up caseTypeDefinitionsCache", reference), e);
                }
            };
        } catch (Exception e) {
            LOG.warn("Error while retrieving all case types references to warm up caseTypeDefinitionsCache", e);
        }
    }

    private HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", authTokenGenerator.generate());
        headers.add(HttpHeaders.AUTHORIZATION, getUserToken());
        return headers;
    }

    private String getUserToken() {
        AuthenticatedUser user = idamHelper.authenticate(applicationParams.getCacheWarmUpEmail(), applicationParams.getCacheWarmUpPassword());
        return user.getAccessToken();
    }
}
