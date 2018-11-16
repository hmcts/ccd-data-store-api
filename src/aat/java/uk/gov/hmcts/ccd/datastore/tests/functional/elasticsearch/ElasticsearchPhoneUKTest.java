package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase.PHONE_UK;

@ExtendWith(ElasticsearchTestDataLoaderExtension.class)
public class ElasticsearchPhoneUKTest extends ElasticsearchBaseTest {

    ElasticsearchPhoneUKTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    static void setUp() {
        assertElasticsearchEnabled();
    }


        @Nested
        @DisplayName("Tests to verify cases on PhoneUK Field")
        class DateTimeField {

            @Test
            @DisplayName("should return case for exact match on a PhoneUK field")
            void shouldReturnCaseForExactMatchOnDateTimeField() {
                searchCaseForExactMatchAndVerifyResponse("PhoneUKField", PHONE_UK);
            }
        }

        private void searchCaseForExactMatchAndVerifyResponse(String field, String value) {
            String jsonSearchRequest = ElasticsearchSearchRequest.exactMatch(CASE_DATA_FIELD_PREFIX + field, value);

            ValidatableResponse response = searchCase(asPrivateCaseworker(false), jsonSearchRequest);

            assertSingleCaseReturned(response);
            assertField(response, RESPONSE_CASE_DATA_FIELDS_PREFIX + field, value);
            assertField(response, CASE_ID, testData.get(EXACT_MATCH_TEST_REFERENCE));
        }

    }



