package uk.gov.hmcts.ccd.v2;

public final class V2 {
    private V2() {}

    public static final String EXPERIMENTAL_HEADER = "experimental";
    public static final String EXPERIMENTAL_WARNING = "Experimental! Subject to change or removal, do not use in production!";

    public final class MediaType {
        private MediaType() {}

        // External API
        public static final String CASE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case.v2+json;charset=UTF-8";
        public static final String CASE_DOCUMENTS = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-documents.v2+json;charset=UTF-8";
        public static final String CREATE_EVENT = "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v2+json;charset=UTF-8";
        public static final String CREATE_CASE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-case.v2+json;charset=UTF-8";
        public static final String START_CASE_EVENT = "application/vnd.uk.gov.hmcts.ccd-data-store-api.start-case-trigger.v2+json;charset=UTF-8";
        public static final String START_EVENT = "application/vnd.uk.gov.hmcts.ccd-data-store-api.start-event-trigger.v2+json;charset=UTF-8";
        public static final String CASE_DATA_VALIDATE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8";

        // Internal API
        public static final String UI_CASE_VIEW = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json;charset=UTF-8";
        public static final String UI_EVENT_VIEW = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-event-view.v2+json;charset=UTF-8";
        public static final String CASE_TYPE_UPDATE_VIEW_EVENT = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-case-trigger.v2+json;charset=UTF-8";
        public static final String CASE_UPDATE_VIEW_EVENT = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-event-trigger.v2+json;charset=UTF-8";
        public static final String UI_START_DRAFT_TRIGGER = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-draft-trigger.v2+json;charset=UTF-8";
        public static final String UI_USER_PROFILE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-user-profile.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_READ = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-read.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_CREATE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-create.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_UPDATE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-update.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_DELETE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-delete.v2+json;charset=UTF-8";
        public static final String UI_WORKBASKET_INPUT_DETAILS =
            "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-workbasket-input-details.v2+json;charset=UTF-8";
        public static final String UI_SEARCH_INPUT_DETAILS =
            "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-search-input-details.v2+json;charset=UTF-8";
        public static final String UI_BANNERS =
            "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-banners.v2+json;charset=UTF-8";
        public static final String UI_JURISDICTION_CONFIGS =
            "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-jurisdiction-configs.v2+json;charset=UTF-8";

        public static final String UI_JURISDICTIONS =
            "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-jurisdictions.v2+json;charset=UTF-8";

        public static final String CASE_EVENTS = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-events.v2+json;charset=UTF-8";

    }

    public final class Error {
        private Error() {}

        public static final String NO_MATCHING_EVENT_TRIGGER = "Cannot find matching event trigger";
        public static final String MISSING_EVENT_TOKEN = "Missing event token";
        public static final String CASE_TYPE_NOT_FOUND = "Case type not found";
        public static final String CASE_NOT_FOUND = "Case not found";
        public static final String USER_ROLE_NOT_FOUND = "User role not found";
        public static final String CASE_DATA_NOT_FOUND = "Case data not found";
        public static final String PRINTABLE_DOCUMENTS_ENDPOINT_DOWN = "Print documents endpoint is down";
        public static final String EVENT_TRIGGER_NOT_FOUND = "Event trigger not found";
        public static final String EVENT_TRIGGER_NOT_SPECIFIED = "Event trigger not specified";
        public static final String EVENT_TRIGGER_NOT_KNOWN_FOR_CASE_TYPE = "Event trigger is not known for given case type";
        public static final String EVENT_TRIGGER_HAS_PRE_STATE = "Event trigger has pre state defined";
        public static final String CASE_ID_INVALID = "Case ID is not valid";
        public static final String CASE_ALTERED = "Case altered outside of transaction";
        public static final String CASE_ROLE_REQUIRED = "Case role missing";
        public static final String CASE_ROLE_INVALID = "Case role does not exist";
        public static final String CASE_ROLE_FORMAT_INVALID = "Case role name format is invalid";
        public static final String GRANT_FORBIDDEN = "Grant action is reserved to users with entire jurisdiction access";
        public static final String CASE_FIELD_INVALID = "Cannot validate case field";
        public static final String CALLBACK_EXCEPTION = "Unsuccessful callback";
        public static final String CASE_AUDIT_EVENTS_NOT_FOUND = "Case audit events not found";
        public static final String ROLES_FOR_CASE_ID_NOT_FOUND = "Cannot find user roles or case roles for the case ID";
        public static final String CASE_TYPE_DEF_NOT_FOUND_FOR_CASE_ID = "Cannot find case type definition for case ID";
        public static final String ERROR_CASE_ID_INVALID = "Case ID is not valid";
        public static final String EMPTY_CASE_ID_LIST = "Case ID list is empty";
        public static final String USER_ID_INVALID = "User ID is not valid";
        public static final String EMPTY_CASE_USER_ROLE_LIST = "Case user roles list is empty";
        public static final String OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED = "Access to other user's case role assignments not granted";
        public static final String NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA = "Not authorised to update case supplementary data";
        public static final String CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION = "Client service not authorised to perform operation";
        public static final String SUPPLEMENTARY_DATA_UPDATE_INVALID = "Supplementary Data Update Invalid";
        public static final String MORE_THAN_ONE_NESTED_LEVEL = "Supplementary data properties with more than one nested level are currently not supported";
        public static final String UNKNOWN_SUPPLEMENTARY_UPDATE_OPERATION = "Unknown supplementary data update operation";
        public static final String ORGANISATION_ID_INVALID = "Organisation ID is not valid";
    }
}
