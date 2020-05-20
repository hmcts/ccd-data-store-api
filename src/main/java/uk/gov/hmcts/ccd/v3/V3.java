package uk.gov.hmcts.ccd.v3;

public final class V3 {
    private V3() { }

    public static final String EXPERIMENTAL_HEADER = "experimental";
    public static final String EXPERIMENTAL_WARNING = "Experimental! Subject to change or removal, do not use in production!";

    public static final class MediaType {
        private MediaType() { }

        // External API
        public static final String CREATE_EVENT = "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v3+json;charset=UTF-8";
        public static final String CREATE_CASE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-case.v3+json;charset=UTF-8";
    }

    public static final class Error {
        private Error() { }

        public static final String NO_MATCHING_EVENT_TRIGGER = "Cannot find matching event trigger";
        public static final String MISSING_EVENT_TOKEN = "Missing event token";
        public static final String CASE_TYPE_NOT_FOUND = "Case type not found";
        public static final String USER_ROLE_NOT_FOUND = "User role not found";
        public static final String CASE_DATA_NOT_FOUND = "Case data not found";
        public static final String EVENT_TRIGGER_NOT_FOUND = "Event trigger not found";
        public static final String EVENT_TRIGGER_NOT_SPECIFIED = "Event trigger not specified";
        public static final String EVENT_TRIGGER_NOT_KNOWN_FOR_CASE_TYPE = "Event trigger is not known for given case type";
        public static final String EVENT_TRIGGER_HAS_PRE_STATE = "Event trigger has pre state defined";
        public static final String CASE_ID_INVALID = "Case ID is not valid";
        public static final String CASE_ALTERED = "Case altered outside of transaction";
        public static final String CASE_FIELD_INVALID = "Cannot validate case field";
        public static final String CALLBACK_EXCEPTION = "Unsuccessful callback";
    }
}
