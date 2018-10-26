package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE_CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchCaseSearchSecurityTest extends ElasticsearchBaseTest {

    private static final String CASE_INDEX_NAME = "aat_private_cases-000001";
    private static final String CASE_INDEX_ALIAS = "aat_private_cases";
    private static final String ES_FIELD_CASE_REFERENCE = "reference";
    private static final String ES_FIELD_CASE_STATE = "state";

    private String caseRefCaseTypeSecurityClassificationQuery;
    private String caseRefCaseStateQuery;

    ElasticsearchCaseSearchSecurityTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    void setUp() {
        assertElasticsearchEnabled();
        importDefinition();
        createCases();
    }

    @Nested
    @DisplayName("Case type security")
    class CaseTypeSecurity {

        @Test
        @DisplayName("should return the case for a role with same security classification as case type classification and read access on case type")
        void shouldReturnCaseForPrivateUser() {
            searchCaseAndAssertCaseReturned(asPrivateCaseworker(false), ES_FIELD_CASE_REFERENCE, caseRefCaseTypeSecurityClassificationQuery);
        }

        @Test
        @DisplayName("should NOT return the case for a role with read access on case type and lower security classification than then case type")
        void shouldNotReturnCaseForPublicUser() {
            searchCaseAndAssertCaseNotReturned(asAutoTestCaseworker(false), ES_FIELD_CASE_REFERENCE, caseRefCaseTypeSecurityClassificationQuery);
        }

        @Test
        @DisplayName("should NOT return the case for a role with same security classification as case type and no read access on case type")
        void shouldNotReturnCaseForRoleWithoutReadAccessToCaseType() {
            //TODO
        }

    }

    @Nested
    @DisplayName("Case state security")
    class CaseStateSecurity {

        @Test
        @DisplayName("should return the case for a role with read access to the case state")
        void shouldReturnCase() {
            searchCaseAndAssertCaseReturned(asPrivateCaseworker(false), ES_FIELD_CASE_REFERENCE, caseRefCaseStateQuery);
        }

        @Test
        @DisplayName("should NOT return the case for a role with no read access to a case state")
        void shouldNotReturnCase() {
            searchCaseAndAssertCaseNotReturned(asAutoTestCaseworker(false), ES_FIELD_CASE_REFERENCE, caseRefCaseStateQuery);
        }

    }

    private void searchCaseAndAssertCaseReturned(Supplier<RequestSpecification> asUser, String field, String value) {
        ValidatableResponse response = searchCase(asUser, field, value);
        response.body("cases.size()", is(1));
        response.body("cases[0]." + field, is(value));
    }

    private void searchCaseAndAssertCaseNotReturned(Supplier<RequestSpecification> asUser, String field, String value) {
        ValidatableResponse response = searchCase(asUser, field, value);
        response.body("cases.size()", is(0));
    }

    private ValidatableResponse searchCase(Supplier<RequestSpecification> asUser, String field, String value) {
        String jsonSearchRequest = createExactMatchSearchRequest(field, value);
        return searchCase(asUser, jsonSearchRequest);
    }

    private String createExactMatchSearchRequest(String field, String value) {
        return "{"
            + "  \"query\": {"
            + "    \"match\": {"
            + "    \"" + field + "\" : \"" + value + "\""
            + "    }"
            + "  }"
            + "}";
    }

    @AfterAll
    void cleanUp() {
        deleteIndexAndAlias(CASE_INDEX_NAME, CASE_INDEX_ALIAS);
    }

    private void createCases() {
        caseRefCaseTypeSecurityClassificationQuery = String.valueOf(createCase(asPrivateCaseworker(true)));
        caseRefCaseStateQuery = String.valueOf(createCaseAndProgressState(asPrivateCaseworker(true)));

        // wait until logstash reads the case data
        sleep(aat.getLogstashReadDelay());
    }

    private Long createCaseAndProgressState(Supplier<RequestSpecification> asUser) {
        Long caseReference = createCase(asUser);
        Event.startProgress(AAT_PRIVATE_CASE_TYPE, caseReference)
            .as(asPrivateCaseworker(true))
            .submit()
            .then()
            .statusCode(201)
            .assertThat()
            .body("state", equalTo(AATCaseType.State.IN_PROGRESS));

        return caseReference;
    }

    private Long createCase(Supplier<RequestSpecification> asUser) {
        return Event.create(AAT_PRIVATE_CASE_TYPE)
            .as(asUser)
            .withData(AATCaseBuilder.EmptyCase.build())
            .submitAndGetReference();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
