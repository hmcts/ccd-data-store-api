package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.junit.AssumptionViolatedException;
import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.ApproximatelyHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.CaseIdAsIntegerHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.CaseIdAsStringHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.ContainsHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.DateGreaterThanTTLGuardDateHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.DateLessThanTTLGuardDateHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.DateThirtyDaysFromTodayHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.DateTwentyDaysFromTodayHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.DocumentIdInTheResponseHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.GenerateUuidHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.HyphenisedCaseIdFromCaseCreationHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.OrganisationsAssignedUsersHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.UniqueStringHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.ValidBinaryLinkHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers.ValidSelfLinkHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private static final Map<String, String> uniqueStringsPerTestData = new ConcurrentHashMap<>();

    private final List<CustomValueHandler> customValueHandlers = List.of(
        new CaseIdAsIntegerHandler(),
        new CaseIdAsStringHandler(),
        new HyphenisedCaseIdFromCaseCreationHandler(),
        new OrganisationsAssignedUsersHandler(),
        new UniqueStringHandler(this),
        new ApproximatelyHandler(),
        new ContainsHandler(),
        new DocumentIdInTheResponseHandler(),
        new ValidSelfLinkHandler(),
        new ValidBinaryLinkHandler(),
        new DateTwentyDaysFromTodayHandler(),
        new DateThirtyDaysFromTodayHandler(),
        new DateGreaterThanTTLGuardDateHandler(),
        new DateLessThanTTLGuardDateHandler(),
        new GenerateUuidHandler()
    );

    @Before("@cdam")
    public void skipDocumentCdamTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("DATA_STORE_CDAM_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("CDAM tests not enabled");
        }
    }
    @Before("@dm-store")
    public void skipDocumentUploadTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("DATA_STORE_DM_STORE_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("DM Store tests not enabled");
        }
    }

    @Before("@drafts")
    public void skipDraftsTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("DATA_STORE_DRAFTS_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Drafts tests not enabled");
        }
    }

    @Before("@elasticsearch")
    public void skipElasticSearchTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("ELASTIC_SEARCH_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Elastic Search tests not enabled");
        }
    }

    @Before
    public void createUID(Scenario scenario) {
        String tag = getDataFileTag(scenario);
        String uid = tag + UUID.randomUUID().toString();
        uniqueStringsPerTestData.put(tag,uid);
    }

    private synchronized String getDataFileTag(Scenario scenario) {
        return scenario.getSourceTagNames().stream()
            .filter(t -> t.startsWith("@S-"))
            .findFirst()
            .map(t -> t.substring(1))
            .map(Object::toString)
            .orElse("error cant find tag");
    }

    public String getUniqueStringsPerScenario(String scenarioTag) {
        return uniqueStringsPerTestData.get(scenarioTag);
    }

    @Override
    protected BeftaTestDataLoader buildTestDataLoader() {
        return new DataLoaderToDefinitionStore(this,
            DataLoaderToDefinitionStore.VALID_CCD_TEST_DEFINITIONS_PATH) {

            @Override
            protected void createRoleAssignment(String resource, String filename) {
                // Do not create role assignments.
                BeftaUtils.defaultLog("Will NOT create role assignments!");
            }

        };
    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        // to add new custom value routine:
        // * create a new CustomValueKey enum value,
        // * create a new CustomValueHandler class,
        // * register handler in customValueHandlers list defined above.
        return customValueHandlers.stream()
            .filter(candidate -> candidate.matches(CustomValueKey.getEnum(key.toString())))
            .findFirst()
            .map(evaluator -> evaluator.calculate(scenarioContext, key))
            .orElseGet(() -> super.calculateCustomValue(scenarioContext, key));
    }

}
