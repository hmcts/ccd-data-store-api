package uk.gov.hmcts.ccd.error;

import org.slf4j.MDC;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Scoped operational dimensions for agent-collected logs; never added to HTTP responses. */
public final class FailureDiagnostics implements AutoCloseable {
    private static final Map<String, String> CODES = Map.ofEntries(
        Map.entry("jakarta.validation.ValidationException", "REQUEST_VALIDATION_FAILED"),
        Map.entry("org.springframework.web.bind.MethodArgumentNotValidException", "REQUEST_VALIDATION_FAILED"),
        Map.entry("org.springframework.validation.BindException", "REQUEST_VALIDATION_FAILED"),
        Map.entry("org.springframework.http.converter.HttpMessageConversionException", "REQUEST_BODY_INVALID"),
        Map.entry("org.springframework.security.core.AuthenticationException", "AUTHENTICATION_FAILED"),
        Map.entry("org.springframework.security.access.AccessDeniedException", "AUTHORISATION_DENIED"),
        Map.entry("java.sql.SQLException", "DATABASE_OPERATION_FAILED"),
        Map.entry("org.springframework.dao.DataAccessException", "DATABASE_OPERATION_FAILED"),
        Map.entry("org.springframework.web.client.RestClientException", "DEPENDENCY_REQUEST_FAILED"),
        Map.entry("feign.FeignException", "DEPENDENCY_REQUEST_FAILED"),
        Map.entry("java.net.SocketException", "DEPENDENCY_REQUEST_FAILED"),
        Map.entry("java.net.SocketTimeoutException", "DEPENDENCY_REQUEST_FAILED"),
        Map.entry("java.net.UnknownHostException", "DEPENDENCY_REQUEST_FAILED")
    );
    private static final Map<String, String> BUSINESS_CODES = Map.ofEntries(
        Map.entry("CaseValidationException", "CASE_VALIDATION_FAILED"),
        Map.entry("EventTokenException", "EVENT_TOKEN_REJECTED"),
        Map.entry("NoCException", "NOC_VALIDATION_FAILED"),
        Map.entry("CaseAssignedUserRoleException", "CASE_ASSIGNMENT_FAILED"),
        Map.entry("CaseCouldNotBeFoundException", "CASE_NOT_FOUND"),
        Map.entry("RequestValidationException", "REQUEST_VALIDATION_FAILED"),
        Map.entry("BadSearchRequest", "SEARCH_REQUEST_INVALID"),
        Map.entry("InvalidImportException", "DEFINITION_IMPORT_FAILED"),
        Map.entry("MapperException", "DEFINITION_IMPORT_FAILED"),
        Map.entry("SpreadsheetParsingException", "DEFINITION_IMPORT_FAILED"),
        Map.entry("EnglishPhraseUniqueConstraintException", "DICTIONARY_PHRASE_EXISTS"),
        Map.entry("CaseIdOrderReferenceUniqueConstraintException", "PAYMENT_ORDER_ALREADY_EXISTS")
    );

    private final String previousCode = MDC.get("failureCode");
    private final String previousStatus = MDC.get("httpStatus");

    private FailureDiagnostics(Throwable failure, int status, String businessCode) {
        MDC.put("failureCode", businessCode == null ? codeFor(failure) : businessCode);
        MDC.put("httpStatus", Integer.toString(status));
    }

    public static FailureDiagnostics open(Throwable failure, int status) {
        return new FailureDiagnostics(failure, status, null);
    }

    public static FailureDiagnostics open(Throwable failure, int status, String businessCode) {
        return new FailureDiagnostics(failure, status, businessCode);
    }

    private static String codeFor(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && visited.size() < 16 && visited.add(cause);
             cause = cause.getCause()) {
            for (Class<?> type = cause.getClass(); type != null; type = type.getSuperclass()) {
                String code = CODES.get(type.getName());
                if (code == null) {
                    code = BUSINESS_CODES.get(type.getSimpleName());
                }
                if (code != null) {
                    return code;
                }
            }
        }
        return "APPLICATION_FAILURE";
    }

    @Override
    public void close() {
        restore("failureCode", previousCode);
        restore("httpStatus", previousStatus);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
