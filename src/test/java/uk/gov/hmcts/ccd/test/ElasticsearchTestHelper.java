package uk.gov.hmcts.ccd.test;

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
    public static final String ADDRESS_FIELD = "AddressUKField";
    public static final String COMPLEX_FIELD = "ComplexField";
    public static final String POST_CODE_FIELD = "PostCode";
    public static final String COUNTY_FIELD = "County";
    public static final String COUNTRY_NESTED_FIELD = "Country";
    public static final String NESTED_COLLECTION_TEXT_FIELD = "NestedCollectionTextField";
    public static final String COMPLEX_NESTED_FIELD = "ComplexNestedField";
    public static final String COMPLEX_TEXT_FIELD = "ComplexTextField";
    public static final String NESTED_NUMBER_FIELD = "NestedNumberField";
    public static final String COMPLEX_FIXED_LIST_FIELD = "ComplexFixedListField";
    public static final String HISTORY_COMPONENT_FIELD = "HistoryComponentField";
    public static final String DOCUMENT_FIELD = "DocumentField";
    public static final String ADDRESS_LINE_1 = "AddressLine1";
    public static final String ADDRESS_LINE_2 = "AddressLine2";
    public static final String ADDRESS_LINE_3 = "AddressLine3";
    public static final String ADDRESS_LINE_3_VALUE = "AddressLine3Value";
    public static final String TOWN_FIELD = "PostTown";
    public static final String STATE = "state";
    public static final String CREATED_DATE = "created_date";
    public static final String TEXT_ALIAS = "TextAlias";
    public static final String FIXED_LIST_ALIAS = "FixedListAlias";

    public static final String AUTOTEST_1 = "AUTOTEST1";
    public static final String DEFAULT_CASE_REFERENCE = "1588866820969121";
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
    public static final String STREET_VALUE = "StreetValue";
    public static final String COMPLEX_TEXT_VALUE = "ComplexTextValue";
    public static final String COUNTY_VALUE = "CountyValue";
    public static final String POST_CODE_VALUE = "PST CDE";
    public static final String ADDRESS_LINE_2_VALUE = "AddressLine2Value";
    public static final String TOWN_VALUE = "TownValue";
    public static final String IN_PROGRESS_STATE = "IN_PROGRESS";
    public static final String CREATED_DATE_VALUE = "2020-05-07T15:53:40.974";
    public static final String LAST_STATE_MODIFIED_DATE_VALUE = "2020-05-07T17:42:00.527";
    public static final String LAST_MODIFIED_DATE_VALUE = "2020-06-09T13:17:06.542";

    public static final String AUTOTEST1_PUBLIC = "caseworker-autotest1";
    public static final String AUTOTEST2_PUBLIC = "caseworker-autotest2";
    public static final String AUTOTEST1_RESTRICTED = "caseworker-autotest1-restricted";
    public static final String AUTOTEST1_PRIVATE = "caseworker-autotest1-private";
    public static final String AUTOTEST1_SOLICITOR = "caseworker-autotest1-solicitor";

    private ElasticsearchTestHelper() { }

    public static String caseData(String fieldPath) {
        return DATA_PREFIX + fieldPath;
    }

    public static String alias(String fieldPath) {
        return ALIAS_PREFIX + fieldPath;
    }

    public static String caseTypesParam(String... caseTypeIds) {
        return String.join(",", caseTypeIds);
    }
}
