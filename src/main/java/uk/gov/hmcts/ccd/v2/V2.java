package uk.gov.hmcts.ccd.v2;

public interface V2 {

    String EXPERIMENTAL_HEADER = "experimental";
    String EXPERIMENTAL_WARNING = "Experimental! Subject to change or removal, do not use in production!";

    interface MediaType {
        String CASE = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case.v2+json;charset=UTF-8";
    }
}
