package uk.gov.hmcts.ccd.domain.service.lau;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
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
import java.util.concurrent.CompletableFuture;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

@Service
@Slf4j
@Qualifier("AuditCaseRemoteOperation")
public class AuditCaseRemoteOperation implements AuditRemoteOperation {

    private static final String BEARER = "Bearer ";
    private static final String LAU_CASE_ACTION_CREATE = "CREATE";
    private static final String LAU_CASE_ACTION_VIEW = "VIEW";
    private static final String LAU_CASE_ACTION_UPDATE = "UPDATE";

    public static final Map<String, String> caseActionMap = Map.of(
        AuditOperationType.CREATE_CASE.getLabel(),LAU_CASE_ACTION_CREATE,
        AuditOperationType.CASE_ACCESSED.getLabel(), LAU_CASE_ACTION_VIEW,
        AuditOperationType.UPDATE_CASE.getLabel(), LAU_CASE_ACTION_UPDATE);

    private final SecurityUtils securityUtils;

    private final AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    private HttpClient httpClient;

    @Autowired
    public AuditCaseRemoteOperation(@Lazy final SecurityUtils securityUtils,
                                    @Qualifier("httpClientAudit") final HttpClient httpClient,
                                    final AuditCaseRemoteConfiguration auditCaseRemoteConfiguration) {
        this.securityUtils = securityUtils;
        this.auditCaseRemoteConfiguration = auditCaseRemoteConfiguration;
        this.httpClient = httpClient;
    }

    @Override
    public CompletableFuture<String> postCaseAction(AuditEntry entry, ZonedDateTime currentDateTime) {

        try {
            if (entry.getOperationType().equals(AuditOperationType.CREATE_CASE.getLabel())
                || entry.getOperationType().equals(AuditOperationType.CASE_ACCESSED.getLabel())
                || entry.getOperationType().equals(AuditOperationType.UPDATE_CASE.getLabel())) {

                ActionLog actionLog = createActionLogFromAuditEntry(entry, currentDateTime);
                String lauCaseAuditUrl = auditCaseRemoteConfiguration.getCaseActionAuditUrl();

                CaseActionPostRequest capr = new CaseActionPostRequest(actionLog);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

                String requestBody = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(capr);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lauCaseAuditUrl))
                    .header("Content-Type", "application/json")
                    .header("ServiceAuthorization", BEARER + securityUtils.getServiceAuthorization())
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                logCorrelationId(entry.getRequestId(), caseActionMap.get(entry.getOperationType()),
                    entry.getJurisdiction(), entry.getIdamId());

                return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body);

            } else {
                log.warn("The operational type " + entry.getOperationType()
                    + " is not recognised as a valid case action.");
            }

        } catch (Exception excep) {
            log.error("Error occured while generating remote log and audit action request.", excep);
        }

        return null;
    }

    @Override
    public CompletableFuture<String> postCaseSearch(AuditEntry entry, ZonedDateTime currentDateTime) {

        try {
            if (entry.getOperationType().equals(AuditOperationType.SEARCH_CASE.getLabel())) {

                SearchLog searchLog = createSearchLogFromAuditEntry(entry, currentDateTime);
                String lauCaseAuditUrl = auditCaseRemoteConfiguration.getCaseSearchAuditUrl();

                CaseSearchPostRequest cspr = new CaseSearchPostRequest(searchLog);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

                String requestBody = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(cspr);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lauCaseAuditUrl))
                    .header("Content-Type", "application/json")
                    .header("ServiceAuthorization", BEARER + securityUtils.getServiceAuthorization())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                logCorrelationId(entry.getRequestId(), "SEARCH", entry.getJurisdiction(), entry.getIdamId());

                return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body);

            } else {
                log.warn("The operational type " + entry.getOperationType()
                    + " is not recognised as a valid case search.");
            }

        } catch (Exception excep) {
            log.error("Error occured while generating remote log and audit search request.", excep);
        }

        return null;
    }

    private ActionLog createActionLogFromAuditEntry(AuditEntry entry, ZonedDateTime currentDateTime) {
        ActionLog actionLog = new ActionLog();
        actionLog.setUserId(entry.getIdamId());
        actionLog.setCaseAction(caseActionMap.get(entry.getOperationType()));
        actionLog.setCaseJurisdictionId(entry.getJurisdiction());
        actionLog.setCaseRef(entry.getCaseId());
        actionLog.setCaseTypeId(entry.getCaseType());
        actionLog.setTimestamp(currentDateTime.format(ISO_INSTANT));
        return actionLog;
    }

    private SearchLog createSearchLogFromAuditEntry(AuditEntry entry, ZonedDateTime currentDateTime) {
        SearchLog searchLog = new SearchLog();
        searchLog.setUserId(entry.getIdamId());
        searchLog.setCaseRefs(entry.getCaseId());
        searchLog.setTimestamp(currentDateTime.format(ISO_INSTANT));
        return searchLog;
    }

    private void logCorrelationId(String requestId, String activity, String jurisdiction, String idamId) {
        log.info("LAU Correlation-ID:REMOTE_LOG_AND_AUDIT_CASE_{},Request-ID:{},jurisdiction:{},idamId:{}",
            activity,
            requestId,
            jurisdiction,
            idamId);
    }

}
