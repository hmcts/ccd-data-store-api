package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.junit.AssumptionViolatedException;
import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.datastore.util.CaseIdHelper.hypheniseACaseId;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private static Map<String, String> uniqueStringsPerTestData = new ConcurrentHashMap<>();

    @Before("@elasticsearch")
    public void skipElasticSearchTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("ELASTIC_SEARCH_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Elastic Search not Enabled");
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
        String docAmUrl = EnvironmentVariableUtils.getRequiredVariable("CASE_DOCUMENT_AM_URL");
        if (key.toString().startsWith("caseIdAsIntegerFrom")) {
            String childContext = key.toString().replace("caseIdAsIntegerFrom_","");
            try {
                return (long) ReflectionUtils.deepGetFieldInObject(scenarioContext,"childContexts." + childContext
                                                                  + ".testData.actualResponse.body.id");
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
        } else if (key.toString().startsWith("HyphenisedCaseIdFromCaseCreation")) {
            String childContext = key.toString().replace("HyphenisedCaseIdFromCaseCreation_","");
            try {
                long longRef = (long) ReflectionUtils.deepGetFieldInObject(
                    scenarioContext,"childContexts." + childContext + ".testData.actualResponse.body.id");
                String result = hypheniseACaseId(Long.toString(longRef));
                return result;
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

            String scenarioTag;
            try {
                scenarioTag = scenarioContext.getParentContext().getCurrentScenarioTag();
            } catch (NullPointerException e) {
                scenarioTag = scenarioContext.getCurrentScenarioTag();
            }
            return uniqueStringsPerTestData.get(scenarioTag);
        } else if (key.toString().startsWith("approximately ")) {
            try {
                String actualSizeFromHeaderStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                        "testData.actualResponse.headers.Content-Length");
                String expectedSizeStr = key.toString().replace("approximately ", "");

                int actualSize =  Integer.parseInt(actualSizeFromHeaderStr);
                int expectedSize = Integer.parseInt(expectedSizeStr);

                if (Math.abs(actualSize - expectedSize) < (actualSize * 10 / 100)) {
                    return actualSizeFromHeaderStr;
                }
                return expectedSize;
            } catch (Exception e) {
                throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
            }
        } else if (key.toString().startsWith("contains ")) {
            try {
                String actualValueStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body.__plainTextValue__");
                String expectedValueStr = key.toString().replace("contains ", "");

                if (actualValueStr.contains(expectedValueStr)) {
                    return actualValueStr;
                }
                return "expectedValueStr " + expectedValueStr + " not present in response ";
            } catch (Exception e) {
                throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
            }
        } else if (key.equals("documentIdInTheResponse")) {
            try {
                String href = (String) ReflectionUtils
                    .deepGetFieldInObject(scenarioContext,
                        "testData.actualResponse.body.documents[0]._links.self.href");
                return href.substring(href.length() - 36);
            } catch (Exception exception) {
                return "Error extracting the Document Id";
            }
        } else if (key.toString().equalsIgnoreCase("validSelfLink")) {
            try {
                String self = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body.documents[0]._links.self.href");
                BeftaUtils.defaultLog("Self: " + self);
                if (self != null && self.startsWith(docAmUrl + "/cases/documents/")) {
                    return self;
                }
                return docAmUrl + "/cases/documents/<a document id>";
            } catch (Exception e) {
                throw new FunctionalTestException("Couldn't get self link from response field", e);
            }

        } else if (key.toString().equalsIgnoreCase("validBinaryLink")) {
            try {
                String binary = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body.documents[0]._links.binary.href");
                BeftaUtils.defaultLog("Binary: " + binary);
                if (binary != null && binary.startsWith(docAmUrl + "/cases/documents/") && binary.endsWith("/binary")) {
                    return binary;
                }
                return docAmUrl + "/cases/documents/<a document id>/binary";
            } catch (Exception e) {
                throw new FunctionalTestException("Couldn't get binary link from response field", e);
            }
        } else if (key.toString().equalsIgnoreCase("dateTwentyDaysFromToday")) {
            return LocalDate.now().plusDays(20).toString();
        } else if (key.toString().equalsIgnoreCase("dateThirtyDaysFromToday")) {
            return LocalDate.now().plusDays(30).toString();
        } else if (key.toString().equalsIgnoreCase("dateGreaterThanTTLGuardDate")) {
            return LocalDate.now().plusYears(10).toString();
        } else if (key.toString().equalsIgnoreCase("dateLessThanTTLGuardDate")) {
            return LocalDate.now().plusDays(10).toString();
        } else if (key.toString().equalsIgnoreCase("generateUUID")) {
            return UUID.randomUUID();
        }
        return super.calculateCustomValue(scenarioContext, key);
    }

    private boolean elasticSearchFunctionalTestsEnabled() {
        return ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
    }

    private Map<String, Object> calculateOrganisationsAssignedUsersPropertyWithValue(
                                                    BackEndFunctionalTestScenarioContext scenarioContext,
                                                    String organisationIdentifierContextPath,
                                                    String previousValueContextPath,
                                                    int incrementBy) {
        String organisationIdentifierFieldPath = organisationIdentifierContextPath
            + ".testData.actualResponse.body.organisationIdentifier";

        try {
            String organisationIdentifier = ReflectionUtils.deepGetFieldInObject(scenarioContext,
                organisationIdentifierFieldPath).toString();
            String propertyName = "orgs_assigned_users." + organisationIdentifier;

            int value = incrementBy; // default

            // if path to previous value supplied : read it
            if (previousValueContextPath != null) {
                String previousValueFieldPath = previousValueContextPath
                                                + ".testData.actualResponse.body.supplementary_data."
                                                + propertyName.replace(".", "\\.");
                Object previousValue = ReflectionUtils.deepGetFieldInObject(scenarioContext, previousValueFieldPath);
                if (previousValue != null) {
                    value = Integer.parseInt(previousValue.toString())  + incrementBy; // and increment
                }
            }
            return Collections.singletonMap(propertyName, value);
        } catch (Exception e) {
            throw new FunctionalTestException("Problem generating 'orgs_assigned_users' supplementary data property.",
                                                                                                                    e);
        }
    }
}
