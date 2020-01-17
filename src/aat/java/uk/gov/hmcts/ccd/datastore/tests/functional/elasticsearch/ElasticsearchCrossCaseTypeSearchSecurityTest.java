package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE2_CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE_CASE_TYPE;

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
public class ElasticsearchCrossCaseTypeSearchSecurityTest extends ElasticsearchBaseTest {

    public static final String NUMBER_FIELD_VALUE = "999999999";
    public static final String AAT_PRIVATE_SECURITY_TEST_REFERENCE = TestData.uniqueReference();
    public static final String AAT_PRIVATE2_SECURITY_TEST_REFERENCE = TestData.uniqueReference();
    public static final String EMAIL_ID_VALUE = "functional@test.com";

    ElasticsearchCrossCaseTypeSearchSecurityTest(AATHelper aat) {
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
        @DisplayName("should return cases only for case types the user has access to - the user role can read case type and has same security classification "
                         + "as case type")
        void shouldReturnCaseForCrossCaseTypeSearch() {
            ValidatableResponse response = searchAcrossCaseTypes(asPrivateCaseworker(false), ES_FIELD_NUMBER_ALIAS, NUMBER_FIELD_VALUE);

            assertSingleCaseReturned(response);
            response.body("cases[0]." + ES_FIELD_CASE_TYPE, is(AAT_PRIVATE_CASE_TYPE));
        }

        @Test
        @DisplayName("should NOT return any cases for a role with read access on case types but lower security classification than the case types")
        void shouldNotReturnCaseForPublicUser() {
            ValidatableResponse response = searchAcrossCaseTypes(asAutoTestCaseworker(false), ES_FIELD_NUMBER_ALIAS, NUMBER_FIELD_VALUE);

            assertNoCaseReturned(response);
        }

        //@Test
        @DisplayName("should NOT return any cases for a role with same security classification as case type and no read access on case type")
        void shouldNotReturnCaseForRoleWithoutReadAccessToCaseType() {
            //TODO
        }


        //@Test
        @DisplayName("should NOT return any cases for a role with lower security classification as case type and no read access on case type")
        void shouldNotReturnCaseForRoleWithLowerClassificationAndNoReadAccessToCaseType() {
            //TODO
        }
    }

    @Nested
    @DisplayName("Case state security")
    class CaseStateSecurity {

        @Test
        @DisplayName("should return the cases for cross case type search for a role with read access to the case states")
        void shouldReturnCase() {
            ValidatableResponse response = searchAcrossCaseTypes(asPrivateCrossCaseTypeCaseworker(false), ES_FIELD_NUMBER_ALIAS, NUMBER_FIELD_VALUE);

            assertCaseReferencesInResponse(response,
                                           testData.get(AAT_PRIVATE_SECURITY_TEST_REFERENCE),
                                           testData.get(AAT_PRIVATE2_SECURITY_TEST_REFERENCE));
            response.body("cases[0]." + ES_FIELD_STATE, is("TODO"));
            response.body("cases[1]." + ES_FIELD_STATE, is("TODO"));
        }

        @Test
        @DisplayName("should NOT return any cases for cross case type search for a role with no read access to a case state")
        void shouldNotReturnCase() {
            ValidatableResponse response = searchAcrossCaseTypes(asPrivateCrossCaseTypeSolicitor(false), ES_FIELD_NUMBER_ALIAS, NUMBER_FIELD_VALUE);

            assertNoCaseReturned(response);
        }

    }

    @Nested
    @DisplayName("Case field security")
    class CaseFieldSecurity {

        @Test
        @DisplayName("should return the case field where user role matches ACL and security classification")
        void shouldReturnCaseField() {
            ValidatableResponse response = searchCaseWithSourceFilter(asRestrictedCrossCaseTypeCaseworker(false), ES_FIELD_NUMBER_ALIAS, NUMBER_FIELD_VALUE,
                                                                      ES_FIELD_NUMBER_ALIAS, ES_FIELD_EMAIL_ALIAS);

            assertCaseListSizeInResponse(response, 2);
            assertCaseReferencesInResponse(response,
                                           testData.get(AAT_PRIVATE_SECURITY_TEST_REFERENCE),
                                           testData.get(AAT_PRIVATE2_SECURITY_TEST_REFERENCE));
            response.body("cases[0].case_data.keySet()", hasSize(2));
            response.body("cases[1].case_data.keySet()", hasSize(2));
            response.body("cases[0].case_data", hasKey("EmailFieldAlias"));
            response.body("cases[1].case_data", hasKey("EmailFieldAlias"));
        }

        @Test
        @DisplayName("should NOT return the case field where user role has lower security classification than case field")
        void shouldNotReturnCaseFieldForLowerSecurityClassification() {
            ValidatableResponse response = searchCaseWithSourceFilter(asPrivateCrossCaseTypeCaseworker(false), ES_FIELD_NUMBER_ALIAS, NUMBER_FIELD_VALUE,
                                                                      ES_FIELD_NUMBER_ALIAS, ES_FIELD_EMAIL_ALIAS);

            assertCaseListSizeInResponse(response, 2);
            assertCaseReferencesInResponse(response,
                                           testData.get(AAT_PRIVATE_SECURITY_TEST_REFERENCE),
                                           testData.get(AAT_PRIVATE2_SECURITY_TEST_REFERENCE));
            response.body("cases[0].case_data.keySet()", hasSize(1));
            response.body("cases[1].case_data.keySet()", hasSize(1));
            response.body("cases[0].case_data", not(hasKey("EmailFieldAlias")));
            response.body("cases[1].case_data", not(hasKey("EmailFieldAlias")));
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

        //@Test
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

    private ValidatableResponse searchCaseWithSourceFilter(Supplier<RequestSpecification> asUser, String field, Object value, String... sourceFilters) {
        String jsonSearchRequest = ElasticsearchSearchRequest.exactMatchWithSourceFilter(field, value, sourceFilters);
        return searchCase(asUser, jsonSearchRequest, AAT_PRIVATE_CASE_TYPE, AAT_PRIVATE2_CASE_TYPE);
    }

}
