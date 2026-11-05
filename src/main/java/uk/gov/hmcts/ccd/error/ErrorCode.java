package uk.gov.hmcts.ccd.error;

/** Stable public categories, independent of exception messages and implementation details. */
public enum ErrorCode {
    INVALID_REQUEST,
    AUTHENTICATION_REQUIRED,
    ACCESS_DENIED,
    RESOURCE_NOT_FOUND,
    METHOD_NOT_ALLOWED,
    REQUEST_TOO_LARGE,
    UNSUPPORTED_MEDIA_TYPE,
    TOO_MANY_REQUESTS,
    INTERNAL_ERROR,
    REQUEST_FAILED;

    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400, 422 -> INVALID_REQUEST;
            case 401 -> AUTHENTICATION_REQUIRED;
            case 403 -> ACCESS_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 413 -> REQUEST_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> TOO_MANY_REQUESTS;
            default -> status >= 500 && status < 600 ? INTERNAL_ERROR : REQUEST_FAILED;
        };
    }
}
