package uk.gov.hmcts.ccd.endpoint.exceptions;

import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponse;

// NB: ResponseStatus attribute not relevant as ResponseStatusCode should override it
public class CallbackFailureWithAssertForUpstreamException extends ApiException {

    private final int reponseStatusCode;

    public CallbackFailureWithAssertForUpstreamException(final CatalogueResponse catalogueResponse, int reponseStatusCode) {
        super(catalogueResponse);
        this.reponseStatusCode = reponseStatusCode;
    }

    public CallbackFailureWithAssertForUpstreamException(final CatalogueResponse catalogueResponse, int reponseStatusCode, String message) {
        super(catalogueResponse, message);
        this.reponseStatusCode = reponseStatusCode;
    }

    public int getResponseStatusCode() {
        return reponseStatusCode;
    }

}
