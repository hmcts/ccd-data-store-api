package uk.gov.hmcts.ccd.v2;

public final class V2 {

    public static final String EXPERIMENTAL_HEADER = "experimental";
    public static final String EXPERIMENTAL_WARNING = "Experimental! Subject to change or removal, do not use in production!";

    public final class MediaType {
        // External API
        public static final String CASE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case.v2+json;charset=UTF-8";
        public static final String START_CASE_TRIGGER = "application/vnd.uk.gov.hmcts.ccd-data-store-api.start-case-trigger.v2+json;charset=UTF-8";
        public static final String START_EVENT_TRIGGER = "application/vnd.uk.gov.hmcts.ccd-data-store-api.start-event-trigger.v2+json;charset=UTF-8";
        public static final String CASE_DATA_VALIDATE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8";

        // Internal API
        public static final String UI_CASE_VIEW = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json;charset=UTF-8";
        public static final String UI_EVENT_VIEW = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-event-view.v2+json;charset=UTF-8";
        public static final String UI_START_CASE_TRIGGER = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-case-trigger.v2+json;charset=UTF-8";
        public static final String UI_START_EVENT_TRIGGER = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-event-trigger.v2+json;charset=UTF-8";
        public static final String UI_START_DRAFT_TRIGGER = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-draft-trigger.v2+json;charset=UTF-8";
        public static final String UI_USER_PROFILE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-user-profile.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_READ = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-read.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_CREATE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-create.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_UPDATE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-update.v2+json;charset=UTF-8";
        public static final String UI_DRAFT_DELETE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-draft-delete.v2+json;charset=UTF-8";
        public static final String UI_WORKBASKET_INPUT_DETAILS =
            "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-workbasket-input-details.v2+json;charset=UTF-8";

    }
}
