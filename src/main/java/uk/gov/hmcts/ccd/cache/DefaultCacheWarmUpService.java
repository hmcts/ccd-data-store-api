package uk.gov.hmcts.ccd.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.idam.IdamHelper;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static uk.gov.hmcts.ccd.AppInsights.CASE_TYPE;
import static uk.gov.hmcts.ccd.AppInsights.CASE_TYPES_REFERENCES;
import static uk.gov.hmcts.ccd.AppInsights.CASE_TYPES_WARM_UP;

@Service
@Qualifier(DefaultCacheWarmUpService.QUALIFIER)
public class DefaultCacheWarmUpService implements CacheWarmUpService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCacheWarmUpService.class);
    public static final String QUALIFIER = "default";

    private final DefaultCaseDefinitionRepository caseDefinitionRepository;
    private final AuthTokenGenerator authTokenGenerator;
    private final ApplicationParams applicationParams;
    private final IdamHelper idamHelper;
    private final AppInsights appInsights;

    public DefaultCacheWarmUpService(final DefaultCaseDefinitionRepository caseDefinitionRepository,
                                     final AuthTokenGenerator authTokenGenerator,
                                     final ApplicationParams applicationParams,
                                     final IdamHelper idamHelper,
                                     final AppInsights appInsights) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.authTokenGenerator = authTokenGenerator;
        this.applicationParams = applicationParams;
        this.idamHelper = idamHelper;
        this.appInsights = appInsights;
    }

    @Async
    @Override
    public void warmUp() {
        Instant start = Instant.now();
        try {
            HttpHeaders httpHeaders = authorizationHeaders();

            Instant startCaseTypesReferences = Instant.now();
            List<String> caseTypesReferences = caseDefinitionRepository.getCaseTypesReferences(httpHeaders);
            final Duration betweenCaseTypesReferences = Duration.between(startCaseTypesReferences, Instant.now());
            appInsights.trackDependency(CASE_TYPES_REFERENCES, "GET", betweenCaseTypesReferences.toMillis(), true);

            for (String reference : caseTypesReferences) {
                TimeUnit.MILLISECONDS.sleep(applicationParams.getCacheWarmUpSleepTime());
                try {
                    Instant startCaseType = Instant.now();
                    caseDefinitionRepository.getCaseType(reference, httpHeaders);
                    final Duration betweenCaseType = Duration.between(startCaseType, Instant.now());
                    appInsights.trackDependency(CASE_TYPE, "GET", betweenCaseType.toMillis(), true);
                } catch (Exception e) {
                    LOG.warn(String.format("Error while retrieving case type %s to warm up caseTypeDefinitionsCache", reference), e);
                }
            };
        } catch (Exception e) {
            LOG.warn("Error while retrieving all case types references to warm up caseTypeDefinitionsCache", e);
        }
        final Duration between = Duration.between(start, Instant.now());
        appInsights.trackDependency(CASE_TYPES_WARM_UP, "GET", between.toMillis(), true);
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
