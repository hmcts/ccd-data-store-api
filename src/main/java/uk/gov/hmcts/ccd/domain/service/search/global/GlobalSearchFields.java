package uk.gov.hmcts.ccd.domain.service.search.global;

import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.domain.types.DynamicListValidator;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.CASE_DATA_PREFIX;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.COLLECTION_VALUE_SUFFIX;

/**
 * Fields used in Global Search.
 **/
public final class GlobalSearchFields {

    private static final String FIELD_SEPARATOR = ".";
    private static final String LIST_VALUE_SUFFIX = FIELD_SEPARATOR + DynamicListValidator.VALUE;

    // metadata
    public static final String REFERENCE = CaseDetailsEntity.REFERENCE_FIELD_COL;
    public static final String JURISDICTION = CaseDetailsEntity.JURISDICTION_FIELD_COL;
    public static final String CASE_TYPE = CaseDetailsEntity.CASE_TYPE_ID_FIELD_COL;
    public static final String STATE = CaseDetailsEntity.STATE_FIELD_COL;
    public static final String CREATED_DATE = CaseDetailsEntity.CREATED_DATE_FIELD_COL;
    public static final String SECURITY_CLASSIFICATION = CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;

    /**
     * Case Data fields used in Global Search.
     */
    public static final class CaseDataFields {

        public static final String CASE_ACCESS_CATEGORY = "CaseAccessCategory";
        public static final String CASE_MANAGEMENT_CATEGORY = "caseManagementCategory";
        public static final String CASE_MANAGEMENT_LOCATION = "caseManagementLocation";
        public static final String CASE_NAME_HMCTS_INTERNAL = "caseNameHmctsInternal";

        public static final String SEARCH_CRITERIA = "SearchCriteria";

        // Hide Utility Class Constructor : Utility class should not have a public or default constructor (squid:S1118)
        private CaseDataFields() {
        }

    }


    /**
     * data.caseManagementLocation fields used in Global Search.
     */
    public static final class CaseManagementLocationFields {

        public static final String BASE_LOCATION = "baseLocation";
        public static final String REGION = "region";

        // Hide Utility Class Constructor : Utility class should not have a public or default constructor (squid:S1118)
        private CaseManagementLocationFields() {
        }

    }


    /**
     * data.SearchCriteria fields used in Global Search.
     */
    public static final class SearchCriteriaFields {

        public static final String SEARCH_PARTIES =  "SearchParties";
        public static final String OTHER_CASE_REFERENCES = "OtherCaseReferences";

        // Hide Utility Class Constructor : Utility class should not have a public or default constructor (squid:S1118)
        private SearchCriteriaFields() {
        }

    }


    /**
     * data.SearchCriteria.SearchParties fields used in Global Search.
     */
    public static final class SearchPartyFields {

        public static final String NAME = "Name";
        public static final String EMAIL_ADDRESS = "EmailAddress";
        public static final String ADDRESS_LINE_1 = "AddressLine1";
        public static final String POSTCODE = "PostCode";
        public static final String DATE_OF_BIRTH = "DateOfBirth";
        public static final String DATE_OF_DEATH = "DateOfDeath";

        // Hide Utility Class Constructor : Utility class should not have a public or default constructor (squid:S1118)
        private SearchPartyFields() {
        }

    }


    /**
     * SupplementaryData fields used in Global Search.
     */
    public static final class SupplementaryDataFields {

        public static final String SERVICE_ID = "HMCTSServiceId";

        // Hide Utility Class Constructor : Utility class should not have a public or default constructor (squid:S1118)
        private SupplementaryDataFields() {
        }

    }


    /**
     * JSON dot notation paths to case data fields used in Global Search.
     */
    public static final class CaseDataPaths {

        // root fields
        public static final String CASE_ACCESS_CATEGORY
            = CASE_DATA_PREFIX + CaseDataFields.CASE_ACCESS_CATEGORY;
        public static final String CASE_MANAGEMENT_CATEGORY
            = CASE_DATA_PREFIX + CaseDataFields.CASE_MANAGEMENT_CATEGORY;
        public static final String CASE_MANAGEMENT_LOCATION
            = CASE_DATA_PREFIX + CaseDataFields.CASE_MANAGEMENT_LOCATION;
        public static final String CASE_NAME_HMCTS_INTERNAL
            = CASE_DATA_PREFIX + CaseDataFields.CASE_NAME_HMCTS_INTERNAL;
        public static final String SEARCH_CRITERIA = CASE_DATA_PREFIX + CaseDataFields.SEARCH_CRITERIA;


        // CaseManagementCategory fields
        private static final String CASE_MANAGEMENT_CATEGORY_PREFIX
            = CASE_MANAGEMENT_CATEGORY + LIST_VALUE_SUFFIX + FIELD_SEPARATOR;

        public static final String CASE_MANAGEMENT_CATEGORY_ID
            = CASE_MANAGEMENT_CATEGORY_PREFIX + DynamicListValidator.CODE;
        public static final String CASE_MANAGEMENT_CATEGORY_NAME
            = CASE_MANAGEMENT_CATEGORY_PREFIX + DynamicListValidator.LABEL;


        // CaseManagementLocation fields
        private static final String CASE_MANAGEMENT_LOCATION_PREFIX = CASE_MANAGEMENT_LOCATION + FIELD_SEPARATOR;

        public static final String BASE_LOCATION
            = CASE_MANAGEMENT_LOCATION_PREFIX + CaseManagementLocationFields.BASE_LOCATION;
        public static final String REGION = CASE_MANAGEMENT_LOCATION_PREFIX + CaseManagementLocationFields.REGION;


        // SearchCriteria fields
        private static final String SEARCH_CRITERIA_PREFIX = SEARCH_CRITERIA + FIELD_SEPARATOR;

        public static final String OTHER_REFERENCE
            = SEARCH_CRITERIA_PREFIX + SearchCriteriaFields.OTHER_CASE_REFERENCES;
        public static final String OTHER_REFERENCE_VALUE = OTHER_REFERENCE + COLLECTION_VALUE_SUFFIX;
        public static final String SEARCH_PARTIES
            = SEARCH_CRITERIA_PREFIX + SearchCriteriaFields.SEARCH_PARTIES + COLLECTION_VALUE_SUFFIX;


        // :: SearchParty fields
        private static final String SEARCH_PARTIES_PREFIX = SEARCH_PARTIES + FIELD_SEPARATOR;

        public static final String SEARCH_PARTY_NAME = SEARCH_PARTIES_PREFIX + SearchPartyFields.NAME;
        public static final String SEARCH_PARTY_EMAIL_ADDRESS = SEARCH_PARTIES_PREFIX + SearchPartyFields.EMAIL_ADDRESS;
        public static final String SEARCH_PARTY_ADDRESS_LINE_1
            = SEARCH_PARTIES_PREFIX + SearchPartyFields.ADDRESS_LINE_1;
        public static final String SEARCH_PARTY_POSTCODE = SEARCH_PARTIES_PREFIX + SearchPartyFields.POSTCODE;
        public static final String SEARCH_PARTY_DATE_OF_BIRTH = SEARCH_PARTIES_PREFIX + SearchPartyFields.DATE_OF_BIRTH;
        public static final String SEARCH_PARTY_DATE_OF_DEATH = SEARCH_PARTIES_PREFIX + SearchPartyFields.DATE_OF_DEATH;


        // Hide Utility Class Constructor : Utility class should not have a public or default constructor (squid:S1118)
        private CaseDataPaths() {
        }

    }


    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private GlobalSearchFields() {
    }

}
