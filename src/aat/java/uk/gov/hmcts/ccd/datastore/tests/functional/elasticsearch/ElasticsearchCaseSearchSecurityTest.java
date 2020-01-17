package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.TestData;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

@ExtendWith(ElasticsearchTestDataLoaderExtension.class)
public class ElasticsearchCaseSearchSecurityTest extends ElasticsearchBaseTest {

    public static final String CASE_TYPE_SECURITY_TEST_REFERENCE = TestData.uniqueReference();
    public static final String CASE_STATE_SECURITY_TEST_REFERENCE = TestData.uniqueReference();
    public static final String CASE_FIELD_SECURITY_TEST_REFERENCE = TestData.uniqueReference();
    public static final String EMAIL_ID_VALUE = "functional@test.com";

    ElasticsearchCaseSearchSecurityTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    static void setUp() {
        assertElasticsearchEnabled();
    }

    @Nested
    @DisplayName("Case type security")
    class CaseTypeSecurity {

        @Test
        @DisplayName("should return the case for a role with same security classification as case type classification and read access on case type")
        void shouldReturnCaseForPrivateUser() {
            searchCaseAndAssertCaseReference(asPrivateCaseworker(false), ES_FIELD_CASE_REFERENCE, testData.get(CASE_TYPE_SECURITY_TEST_REFERENCE));
        }

        @Test
        @DisplayName("should NOT return the case for a role with read access on case type and lower security classification than then case type")
        void shouldNotReturnCaseForPublicUser() {
            searchCaseAndAssertCaseNotReturned(asAutoTestCaseworker(false), ES_FIELD_CASE_REFERENCE, testData.get(CASE_TYPE_SECURITY_TEST_REFERENCE));
        }

        //@Test
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
            searchCaseAndAssertCaseReference(asPrivateCaseworker(false), ES_FIELD_CASE_REFERENCE, testData.get(CASE_STATE_SECURITY_TEST_REFERENCE));
        }

        @Test
        @DisplayName("should NOT return the case for a role with no read access to a case state")
        void shouldNotReturnCase() {
            searchCaseAndAssertCaseNotReturned(asPrivateCaseworkerSolicitor(false), ES_FIELD_CASE_REFERENCE, testData.get(CASE_STATE_SECURITY_TEST_REFERENCE));
        }

    }

    @Nested
    @DisplayName("Case field security")
    class CaseFieldSecurity {

        @Test
        @DisplayName("should return the case field where user role matches ACL and security classification")
        void shouldReturnCaseField() {
            ValidatableResponse response = searchCaseAndAssertCaseReference(asRestrictedCaseworker(false),
                                                                            ES_FIELD_CASE_REFERENCE,
                                                                            testData.get(CASE_FIELD_SECURITY_TEST_REFERENCE));
            response.body("cases[0]." + RESPONSE_CASE_DATA_FIELDS_PREFIX + ES_FIELD_EMAIL_ID, is(EMAIL_ID_VALUE));
        }

        @Test
        @DisplayName("should NOT return the case field where user role has lower security classification than case field")
        void shouldNotReturnCaseFieldForLowerSecurityClassification() {
            ValidatableResponse response = searchCaseAndAssertCaseReference(asPrivateCaseworker(false),
                                                                            ES_FIELD_CASE_REFERENCE,
                                                                            testData.get(CASE_FIELD_SECURITY_TEST_REFERENCE));
            response.body("cases[0].case_data", not(hasKey(ES_FIELD_EMAIL_ID)));
        }

        //@Test
        @DisplayName("should NOT return the case field where user role does not have read access on the field")
        void shouldNotReturnCaseFieldForNoReadAccess() {
            //TODO
        }

    }

    @Nested
    @DisplayName("User access security")
    class UserAccessSecurity {

        @Test
        @DisplayName("should return the case for a solicitor role if granted access to the case")
        void shouldReturnCase() {
            //TODO
        }

        //@Test
        @DisplayName("should NOT return the case for a solicitor role if not granted access to the case")
        void shouldNotReturnCase() {
            //TODO
        }

    }

    private ValidatableResponse searchCaseAndAssertCaseReference(Supplier<RequestSpecification> asUser, String field, Object value) {
        ValidatableResponse response = searchCase(asUser, field, value);
        assertSingleCaseReturned(response);
        assertField(response, CASE_ID, value);
        return response;
    }

    private void searchCaseAndAssertCaseNotReturned(Supplier<RequestSpecification> asUser, String field, Object value) {
        ValidatableResponse response = searchCase(asUser, field, value);
        assertNoCaseReturned(response);
    }

    private ValidatableResponse searchCase(Supplier<RequestSpecification> asUser, String field, Object value) {
        String jsonSearchRequest = ElasticsearchSearchRequest.exactMatch(field, value);
        return searchCase(asUser, jsonSearchRequest);
    }

}
