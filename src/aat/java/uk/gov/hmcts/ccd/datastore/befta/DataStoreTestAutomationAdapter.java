package uk.gov.hmcts.ccd.datastore.befta;

import com.google.common.collect.MapMaker;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.junit.AssumptionViolatedException;
import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.befta.util.Retryable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.datastore.util.CaseIdHelper.hypheniseACaseId;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    // Weak identity keys keep scenarios isolated and allow completed contexts to be collected.
    private static final Map<BackEndFunctionalTestScenarioContext, String> UNIQUE_STRINGS_PER_SCENARIO =
        new MapMaker().weakKeys().makeMap();

    private static final ReentrantLock GLOBAL_SEARCH_SCENARIO_LOCK = new ReentrantLock();
    private static final ReentrantLock CLOSED_CASES_SCENARIO_LOCK = new ReentrantLock();

    @Before("@F-1005")
    public void serializeGlobalSearchScenarios() {
        GLOBAL_SEARCH_SCENARIO_LOCK.lock();
    }

    @After("@F-1005")
    public void releaseGlobalSearchScenarioLock() {
        if (GLOBAL_SEARCH_SCENARIO_LOCK.isHeldByCurrentThread()) {
            GLOBAL_SEARCH_SCENARIO_LOCK.unlock();
        }
    }

    @Before("@F-942 or @F-944 or @F-945")
    public void serializeClosedCaseScenarios() {
        CLOSED_CASES_SCENARIO_LOCK.lock();
    }

    @After("@F-942 or @F-944 or @F-945")
    public void releaseClosedCaseScenarioLock() {
        if (CLOSED_CASES_SCENARIO_LOCK.isHeldByCurrentThread()) {
            CLOSED_CASES_SCENARIO_LOCK.unlock();
        }
    }

    @Before("@elasticsearch")
    public void skipElasticSearchTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("ELASTIC_SEARCH_FTA_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Elastic Search not Enabled");
        }
    }

    @Before("@groupaccess")
    public void skipGroupAccessTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("GROUP_ACCESS_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Group Access not Enabled");
        }
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

            @Override
            public synchronized void loadDataIfNotLoadedVeryRecently() {
                super.loadDataIfNotLoadedVeryRecently();
                IdamTestingSupportUserCreator.createUsersInIdam();
            }

        };
    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String readinessPrefix = "caseReferenceForGlobalSearchReadiness_";
        if (key.toString().startsWith(readinessPrefix)) {
            return configureGlobalSearchReadiness(scenarioContext, key.toString().substring(readinessPrefix.length()));
        }
        if ("UniqueString".equals(key)) {
            BackEndFunctionalTestScenarioContext rootContext = scenarioContext;
            while (rootContext.getParentContext() != null) {
                rootContext = rootContext.getParentContext();
            }
            return UNIQUE_STRINGS_PER_SCENARIO.computeIfAbsent(
                rootContext, context -> context.getCurrentScenarioTag() + UUID.randomUUID());
        }
        if ("validBuildingLocation".equals(key)) {
            return selectValidBuildingLocation(scenarioContext.getTestData().getActualResponse()
                                                   .getBody().get("arrayInMap"));
        }
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
        } else if (key.toString().startsWith("contains ") && key.toString().contains(" and does not contain ")) {
            try {
                String actualValueStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body.__plainTextValue__");
                String[] expectedValues = key.toString().split(" and does not contain ", 2);
                String expectedPresentValueStr = expectedValues[0].replace("contains ", "");
                String expectedAbsentValueStr = expectedValues[1];

                if (actualValueStr.contains(expectedPresentValueStr)
                    && !actualValueStr.contains(expectedAbsentValueStr)) {
                    return actualValueStr;
                }
                return "response did not match plain text expectations";
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

    private String configureGlobalSearchReadiness(BackEndFunctionalTestScenarioContext context, String creationId) {
        // Only this read-only search probe may retry POSTs; case creation and the scenario keep their own policies.
        if (!"POST".equals(context.getTestData().getMethod())
            || !"/globalSearch".equals(context.getTestData().getUri())) {
            throw new FunctionalTestException("Global search readiness requires a POST /globalSearch probe");
        }
        String caseReference = context.getParentContext().getChildContexts().get(creationId)
            .getTestData().getActualResponse().getBody().get("id").toString();
        BeftaUtils.defaultLog("Waiting for global search case " + caseReference
                                  + " with up to 11 attempts, 3 seconds apart");
        context.setRetryableTag(Retryable.builder()
            .maxAttempts(11)
            .delay(3000)
            .statusCodes(Set.of())
            .match(List.of("\"casesReturned\"\\s*:\\s*0\\s*[,}]"))
            .nonRetryableHttpMethods(Set.of())
            .retryListener(Retryable.setRetryListener(true))
            .build());
        return caseReference;
    }

    private Map<?, ?> selectValidBuildingLocation(Object locations) {
        if (locations instanceof List<?>) {
            // Reference data is unordered and can contain buildings without a region.
            // Always select the same complete record for case creation, criteria and assertions.
            for (Object location : (List<?>) locations) {
                if (location instanceof Map<?, ?>) {
                    Map<?, ?> building = (Map<?, ?>) location;
                    boolean complete = Arrays.asList("epimms_id", "building_location_name", "region_id", "region")
                        .stream().allMatch(field -> {
                            Object value = building.get(field);
                            return value != null && !value.toString().isBlank()
                                && !"null".equalsIgnoreCase(value.toString().trim());
                        });
                    if (complete) {
                        return building;
                    }
                }
            }
        }
        throw new FunctionalTestException("No building location with non-blank epimms_id, building_location_name, "
                                              + "region_id and region found in location reference data");
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
