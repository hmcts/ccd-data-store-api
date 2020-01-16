package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponse;

import java.io.Serializable;
import java.util.List;

@ResponseStatus(
    code = HttpStatus.UNPROCESSABLE_ENTITY
)
public class ApiException extends RuntimeException {

    private final transient CatalogueResponse catalogueResponse;
    private Serializable details;
    private List<String> callbackWarnings;
    private List<String> callbackErrors;

    public ApiException(CatalogueResponse catalogueResponse, String message) {
        super(message);
        this.catalogueResponse = catalogueResponse;
    }

    public ApiException(CatalogueResponse catalogueResponse) {
        this(catalogueResponse, catalogueResponse.getMessage());
    }

    public ApiException(String message) {
        super(message);
        this.catalogueResponse = null;
    }

    public ApiException(final String message, final Throwable e) {
        super(message, e);
        this.catalogueResponse = null;
    }

    public ApiException withDetails(Serializable details) {
        this.details = details;
        return this;
    }

    public Serializable getDetails() {
        return details;
    }

    public List<String> getCallbackWarnings() {
        return callbackWarnings;
    }

    public ApiException withWarnings(List<String> warnings) {
        this.callbackWarnings = warnings;
        return this;
    }

    public List<String> getCallbackErrors() {
        return callbackErrors;
    }

    public ApiException withErrors(List<String> errors) {
        this.callbackErrors = errors;
        return this;
    }

    public CatalogueResponse getCatalogueResponse() {
        return catalogueResponse;
    }
}
