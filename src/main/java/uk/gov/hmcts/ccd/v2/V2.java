package uk.gov.hmcts.ccd.v2;

public final class V2 {

    public static final String EXPERIMENTAL_HEADER = "experimental";
    public static final String EXPERIMENTAL_WARNING = "Experimental! Subject to change or removal, do not use in production!";

    public final class MediaType {
        // External API
        public static final String CASE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case.v2+json;charset=UTF-8";

        // Internal API
        public static final String UI_CASE_VIEW = "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json;charset=UTF-8";
    }
}
