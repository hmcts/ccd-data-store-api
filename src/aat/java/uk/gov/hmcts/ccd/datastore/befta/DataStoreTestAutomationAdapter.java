package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    @Override
    public void doLoadTestData() {
        if (elasticSearchEnabled()) {
            new ElasticsearchTestDataLoaderExtension().deleteIndexesIfPresent();
        }
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
        } else if (key.toString().startsWith("orgsAssignedUsers")) {
            // extract args from key
            //    0 - path to context holding organisationIdentifier
            //    1 - (Optional) path to context holding previous value to use (otherwise: use 0)
            //    2 - (Optional) amount to increment previous value by (otherwise: don't increment)
            List<String> args = Arrays.asList(key.toString().replace("orgsAssignedUsers_","").split("\\|"));
            String organisationIdentifierContextPath = args.get(0);
            String previousValueContextPath = args.size() > 1 ? args.get(1) : null;
            int incrementBy = args.size() > 2 ? Integer.parseInt(args.get(2)) : 0;

            return calculateOrganisationsAssignedUsersPropertyWithValue(scenarioContext,
                                                                        organisationIdentifierContextPath,
                                                                        previousValueContextPath,
                                                                        incrementBy);
        } else if (key.toString().equals("UniqueString")) {
            return ScenarioData.getUniqueString();
        }
        return super.calculateCustomValue(scenarioContext, key);
    }

    private boolean elasticSearchEnabled() {
        return ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
    }

    private Map<String, Object> calculateOrganisationsAssignedUsersPropertyWithValue(BackEndFunctionalTestScenarioContext scenarioContext,
                                                                                     String organisationIdentifierContextPath,
                                                                                     String previousValueContextPath,
                                                                                     int incrementBy) {
        String organisationIdentifierFieldPath = organisationIdentifierContextPath + ".testData.actualResponse.body.organisationIdentifier";

        try {
            String organisationIdentifier = ReflectionUtils.deepGetFieldInObject(scenarioContext, organisationIdentifierFieldPath).toString();
            String propertyName = "orgs_assigned_users." + organisationIdentifier;

            int value = 0; // default

            // if path to previous value supplied : read it
            if (previousValueContextPath != null) {
                String previousValueFieldPath = previousValueContextPath + ".testData.actualResponse.body.supplementary_data."
                                                                         + propertyName.replace(".", "\\.");
                Object previousValue = ReflectionUtils.deepGetFieldInObject(scenarioContext, previousValueFieldPath);
                if (previousValue != null) {
                    value = Integer.parseInt(previousValue.toString())  + incrementBy; // and increment
                }  else {
                    throw new FunctionalTestException("Cannot find previous supplementary data property: '" + previousValueFieldPath + "'");
                }
            }
            return Collections.singletonMap(propertyName, value);
        } catch (Exception e) {
            throw new FunctionalTestException("Problem generating 'orgs_assigned_users' supplementary data property.", e);
        }
    }
}
