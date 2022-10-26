package uk.gov.hmcts.ccd.domain.service.lau;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.AuditCaseRemoteConfiguration;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.lau.ActionLog;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.SearchLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@Qualifier("AuditCaseRemoteOperation")
public class AuditCaseRemoteOperation implements AuditRemoteOperation {

    private static final String LAU_CASE_ACTION_CREATE = "CREATE";
    private static final String LAU_CASE_ACTION_VIEW = "VIEW";
    private static final String LAU_CASE_ACTION_UPDATE = "UPDATE";

    public static final Map<String, String> CASE_ACTION_MAP = Map.of(
        AuditOperationType.CREATE_CASE.getLabel(),LAU_CASE_ACTION_CREATE,
        AuditOperationType.CASE_ACCESSED.getLabel(), LAU_CASE_ACTION_VIEW,
        AuditOperationType.UPDATE_CASE.getLabel(), LAU_CASE_ACTION_UPDATE);

    private final SecurityUtils securityUtils;

    private final AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public AuditCaseRemoteOperation(@Lazy final SecurityUtils securityUtils,
                                    @Qualifier("httpClientAudit") final HttpClient httpClient,
                                    @Qualifier("SimpleObjectMapper") final ObjectMapper objectMapper,
                                    final AuditCaseRemoteConfiguration auditCaseRemoteConfiguration) {
        this.securityUtils = securityUtils;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.auditCaseRemoteConfiguration = auditCaseRemoteConfiguration;
    }

    @Override
    public void postCaseAction(AuditEntry entry, ZonedDateTime currentDateTime) {

        try {
            if (CASE_ACTION_MAP.containsKey(entry.getOperationType())) {

                ActionLog actionLog = createActionLogFromAuditEntry(entry, currentDateTime);
                String lauCaseAuditUrl = auditCaseRemoteConfiguration.getCaseActionAuditUrl();

                CaseActionPostRequest capr = new CaseActionPostRequest(actionLog);
                String activity = CASE_ACTION_MAP.get(entry.getOperationType());
                String requestBody = objectMapper
                    .writeValueAsString(capr);

                postAsyncAuditRequestAndHandleResponse(entry, activity, requestBody, lauCaseAuditUrl);

            } else {
                log.warn("The operational type " + entry.getOperationType()
                    + " is not recognised as a valid case action.");
            }

        } catch (Exception excep) {
            log.error("Error occurred while generating remote log and audit action request. ", excep);
        }

    }

    @Override
    public void postCaseSearch(AuditEntry entry, ZonedDateTime currentDateTime) {

        try {
            if (entry.getOperationType().equals(AuditOperationType.SEARCH_CASE.getLabel())) {

                SearchLog searchLog = createSearchLogFromAuditEntry(entry, currentDateTime);
                String lauCaseAuditUrl = auditCaseRemoteConfiguration.getCaseSearchAuditUrl();

                CaseSearchPostRequest cspr = new CaseSearchPostRequest(searchLog);
                String activity = "SEARCH";
                String requestBody = objectMapper
                    .writeValueAsString(cspr);

                postAsyncAuditRequestAndHandleResponse(entry, activity, requestBody, lauCaseAuditUrl);

            } else {
                log.warn("The operational type " + entry.getOperationType()
                    + " is not recognised as a valid case search.");
            }

        } catch (Exception excep) {
            log.error("Error occurred while generating remote log and audit search request.", excep);
        }
    }

    private void postAsyncAuditRequestAndHandleResponse(AuditEntry entry, String activity, String body, String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("ServiceAuthorization", securityUtils.getServiceAuthorization())
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        String auditLogId = UUID.randomUUID().toString();

        logCorrelationId(entry.getRequestId(), activity, entry.getJurisdiction(), entry.getIdamId(), auditLogId);

        CompletableFuture<HttpResponse<String>> responseFuture =
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        responseFuture.whenComplete((response, error) -> {
            if (response != null) {
                logAuditResponse(entry.getRequestId(), activity, response.statusCode(), request.uri(), auditLogId);
            }
            if (error != null) {
                log.error("Error occurred while processing response for remote log and audit request. ", error);
            }
        });
    }

    private ActionLog createActionLogFromAuditEntry(AuditEntry entry, ZonedDateTime zonedDateTime) {
        ActionLog actionLog = new ActionLog();
        actionLog.setUserId(entry.getIdamId());
        actionLog.setCaseAction(CASE_ACTION_MAP.get(entry.getOperationType()));
        actionLog.setCaseJurisdictionId(entry.getJurisdiction());
        actionLog.setCaseRef(entry.getCaseId());
        actionLog.setCaseTypeId(entry.getCaseType());
        actionLog.setTimestamp(zonedDateTime);
        return actionLog;
    }

    private SearchLog createSearchLogFromAuditEntry(AuditEntry entry, ZonedDateTime zonedDateTime) {
        SearchLog searchLog = new SearchLog();
        searchLog.setUserId(entry.getIdamId());
        searchLog.setCaseRefs(entry.getCaseId());
        searchLog.setTimestamp(zonedDateTime);
        return searchLog;
    }

    private void logCorrelationId(
        String requestId, String activity, String jurisdiction, String idamId, String auditLogId) {
        log.info("LAU Correlation-ID:REMOTE_LOG_AND_AUDIT_CASE_{},Request-ID:{},jurisdiction:{},idamId:{}, logId:{}",
            activity,
            requestId,
            jurisdiction,
            idamId,
            auditLogId);
    }

    private void logAuditResponse(
        String requestId, String activity, int httpStatus, URI uri, String auditLogId) {
        log.info("LAU Response:REMOTE_LOG_AND_AUDIT_CASE_{},Request-ID:{},httpStatus:{},url:{}, logId:{}",
            activity,
            requestId,
            httpStatus,
            uri.toString(),
            auditLogId);
    }
}
