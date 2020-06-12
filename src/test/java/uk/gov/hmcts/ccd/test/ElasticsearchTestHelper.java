package uk.gov.hmcts.ccd.test;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class ElasticsearchTestHelper {

    public static final String DATA_PREFIX = "data.";
    public static final String ALIAS_PREFIX = "alias.";
    public static final String VALUE_SUFFIX = ".value";
    public static final String CASE_TYPE_A = "AAT";
    public static final String CASE_TYPE_B = "MAPPER";
    public static final String CASE_TYPE_C = "SECURITY";

    public static final String NUMBER_FIELD = "NumberField";
    public static final String YES_OR_NO_FIELD = "YesOrNoField";
    public static final String TEXT_FIELD = "TextField";
    public static final String DATE_FIELD = "DateField";
    public static final String PHONE_FIELD = "PhoneUKField";
    public static final String COUNTRY_FIELD = "AddressUKField.Country";
    public static final String COLLECTION_FIELD = "CollectionField";
    public static final String DATE_TIME_FIELD = "DateTimeField";
    public static final String EMAIL_FIELD = "EmailField";
    public static final String FIXED_LIST_FIELD = "FixedListField";
    public static final String FIXED_RADIO_LIST_FIELD = "FixedRadioListField";
    public static final String MONEY_FIELD = "MoneyGBPField";
    public static final String MULTI_SELECT_LIST_FIELD = "MultiSelectListField";
    public static final String TEXT_AREA_FIELD = "TextAreaField";
    public static final String STATE = "state";
    public static final String CREATED_DATE = "created_date";
    public static final String TEXT_ALIAS = "TextAlias";
    public static final String FIXED_LIST_ALIAS = "FixedListAlias";

    public static final String YES_OR_NO_VALUE = "No";
    public static final String NUMBER_VALUE = "12345";
    public static final String TEXT_VALUE = "AAA TextValue";
    public static final String DATE_VALUE = "2007-12-17";
    public static final String PHONE_VALUE = "01234567890";
    public static final String COUNTRY_VALUE = "CountryValue";
    public static final String COLLECTION_VALUE = "CollectionTextValue2";
    public static final String STATE_VALUE = "TODO";
    public static final String DATE_TIME_VALUE = "2003-02-01T12:30:00.000";
    public static final String EMAIL_VALUE = "email1@gmail.com";
    public static final String FIXED_LIST_VALUE = "VALUE2";
    public static final String MONEY_VALUE = "98700";
    public static final String TEXT_AREA_VALUE = "TextArea\nValue";
    public static final String IN_PROGRESS_STATE = "IN_PROGRESS";

    public static final String AUTOTEST1_PUBLIC = "caseworker-autotest1";
    public static final String AUTOTEST2_PUBLIC = "caseworker-autotest2";
    public static final String AUTOTEST1_RESTRICTED = "caseworker-autotest1-restricted";
    public static final String AUTOTEST1_PRIVATE = "caseworker-autotest1-private";

    private ElasticsearchTestHelper() { }

    public static String caseData(String fieldPath) {
        return DATA_PREFIX + fieldPath;
    }

    public static String alias(String fieldPath) {
        return ALIAS_PREFIX + fieldPath;
    }

    public static void assertExampleCaseMetadata(CaseDetails caseDetails) {
        assertAll(
            () -> assertThat(caseDetails.getJurisdiction(), is("AUTOTEST1")),
            () -> assertThat(caseDetails.getCaseTypeId(), is(CASE_TYPE_A)),
            () -> assertThat(caseDetails.getCreatedDate().toString(), is("2020-05-07T15:53:40.974")),
            () -> assertThat(caseDetails.getLastModified().toString(), is("2020-06-09T13:17:06.542")),
            // () -> assertThat(caseDetails.getLastStateModifiedDate().toString(), is("TBC")), // TODO: After RDM-8552 available
            () -> assertThat(caseDetails.getReference(), is(1588866820969121L)),
            () -> assertThat(caseDetails.getState(), is(STATE_VALUE)),
            () -> assertThat(caseDetails.getSecurityClassification(), is(SecurityClassification.PUBLIC))
        );
    }

    public static void assertExampleCaseData(CaseDetails caseDetails) {
        Map<String, JsonNode> data = caseDetails.getData();
        assertAll(
            () -> assertThat(data.get("AddressUKField").toString(),
                is("{\"AddressLine1\":\"StreetValue\","
                   + "\"AddressLine2\":\"AddressLine2Value\","
                   + "\"AddressLine3\":\"AddressLine3Value\","
                   + "\"Country\":\"CountryValue\","
                   + "\"County\":\"CountyValue\","
                   + "\"PostCode\":\"PST CDE\","
                   + "\"PostTown\":\"TownValue\"}")),
            () -> assertThat(data.get(COLLECTION_FIELD).toString(),
                is("[{\"id\":\"2c6da07c-1dfb-4765-88f6-96cd5d5f33b1\",\"value\":\"CollectionTextValue2\"},"
                   + "{\"id\":\"f7d67f03-172d-4adb-85e5-ca958ad442ce\",\"value\":\"CollectionTextValue1\"}]")),
            () -> assertThat(data.get("ComplexField").toString(),
                is("{\"ComplexFixedListField\":\"VALUE3\""
                   + ",\"ComplexNestedField\":{\"NestedCollectionTextField\":"
                   + "[{\"id\":\"8e19ccb3-2d8c-42f0-abe1-fa585cc2d8c8\",\"value\":\"NestedCollectionTextValue1\"},"
                   + "{\"id\":\"95f337e8-5f17-4b25-a795-b7f84f4b2855\",\"value\":\"NestedCollectionTextValue2\"}],"
                   + "\"NestedNumberField\":\"567\"},"
                   + "\"ComplexTextField\":\"ComplexTextValue\"}")),
            () -> assertThat(data.get(DATE_FIELD).asText(), is(DATE_VALUE)),
            () -> assertThat(data.get(DATE_TIME_FIELD).asText(), is(DATE_TIME_VALUE)),
            () -> assertThat(data.get(EMAIL_FIELD).asText(), is(EMAIL_VALUE)),
            () -> assertThat(data.get(FIXED_LIST_FIELD).asText(), is(FIXED_LIST_VALUE)),
            () -> assertThat(data.get(FIXED_RADIO_LIST_FIELD).isNull(), is(true)),
            () -> assertThat(data.get(MONEY_FIELD).asText(), is(MONEY_VALUE)),
            () -> assertThat(data.get(MULTI_SELECT_LIST_FIELD).toString(), is("[\"OPTION2\",\"OPTION4\"]")),
            () -> assertThat(data.get(NUMBER_FIELD).asText(), is(NUMBER_VALUE)),
            () -> assertThat(data.get(PHONE_FIELD).asText(), is(PHONE_VALUE)),
            () -> assertThat(data.get(TEXT_AREA_FIELD).asText(), is(TEXT_AREA_VALUE)),
            () -> assertThat(data.get(TEXT_FIELD).asText(), is(TEXT_VALUE)),
            () -> assertThat(data.get(YES_OR_NO_FIELD).asText(), is(YES_OR_NO_VALUE))
        );
    }
}
