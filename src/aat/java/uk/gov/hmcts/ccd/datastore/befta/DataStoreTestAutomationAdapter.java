package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.Before;
import org.junit.AssumptionViolatedException;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.es.ElasticsearchTestDataLoaderExtension;

import java.util.UUID;

import static java.util.Optional.ofNullable;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    private static String uniqueString;

    @Before
    public void createUID() {
        uniqueString = UUID.randomUUID().toString();
    }

    @Before("@elasticsearch")
    public void skipElasticSearchTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("ELASTIC_SEARCH_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Elastic Search not Enabled");
        }
    }

    @Override
    public void doLoadTestData() {
        loader.addCcdRoles();
        loader.importDefinitions();
    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        if (key.toString().startsWith("caseIdAsIntegerFrom")) {
            String childContext = key.toString().replace("caseIdAsIntegerFrom_","");
            try {
                return (long) ReflectionUtils.deepGetFieldInObject(scenarioContext,"childContexts." + childContext + ".testData.actualResponse.body.id");
            } catch (Exception e) {
                throw new FunctionalTestException("Problem getting case id as long", e);
            }
        } else if (key.toString().startsWith("caseIdAsStringFrom")) {
            String childContext = key.toString().replace("caseIdAsStringFrom_","");
            try {
                long longRef = (long) ReflectionUtils.deepGetFieldInObject(
                    scenarioContext,"childContexts." + childContext + ".testData.actualResponse.body.id");
                return Long.toString(longRef);
            } catch (Exception e) {
                throw new FunctionalTestException("Problem getting case id as long", e);
            }
        } else if (key.toString().equals("UniqueString")) {
            return uniqueString;
        }
        return super.calculateCustomValue(scenarioContext, key);
    }

    private boolean elasticSearchEnabled() {
        return ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
    }
}
