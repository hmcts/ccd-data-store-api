package uk.gov.hmcts.ccd.domain.service.lau;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.LogAndAuditFeignClient;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostResponse;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostResponse;

import java.net.URI;
import java.util.UUID;

@Service
@Slf4j
public class AsyncAuditRequestService {

    private static final String LAU_CASE_ACTION_CREATE = "CREATE";
    private static final String LAU_CASE_ACTION_VIEW = "VIEW";
    private static final String LAU_CASE_ACTION_UPDATE = "UPDATE";

    private final LogAndAuditFeignClient logAndAuditFeignClient;
    private final SecurityUtils securityUtils;

    @Autowired
    public AsyncAuditRequestService(LogAndAuditFeignClient logAndAuditFeignClient,
                                    SecurityUtils securityUtils) {
        this.logAndAuditFeignClient = logAndAuditFeignClient;
        this.securityUtils = securityUtils;
    }

    @Async("taskExecutor")
    public void postAsyncAuditRequestAndHandleResponse(
        AuditEntry entry,
        String activity,
        CaseActionPostRequest capr,
        CaseSearchPostRequest cspr,
        String url) {

        String auditLogId = UUID.randomUUID().toString();

        try {
            logCorrelationId(entry.getRequestId(), activity,
                entry.getJurisdiction(), entry.getIdamId(), auditLogId);

            if (LAU_CASE_ACTION_CREATE.equals(activity) || LAU_CASE_ACTION_UPDATE.equals(activity)
                || LAU_CASE_ACTION_VIEW.equals(activity)) {
                ResponseEntity<CaseActionPostResponse> caseResponse = logAndAuditFeignClient
                    .postCaseAction(securityUtils.getServiceAuthorization(), capr);
                if (caseResponse != null) {
                    logAuditResponse(entry.getRequestId(), activity, caseResponse.getStatusCode().value(),
                        URI.create(url), auditLogId);
                }
            } else if ("SEARCH".equals(activity)) {
                ResponseEntity<CaseSearchPostResponse> searchResponse = logAndAuditFeignClient
                    .postCaseSearch(securityUtils.getServiceAuthorization(), cspr);
                if (searchResponse != null) {
                    logAuditResponse(entry.getRequestId(), activity, searchResponse.getStatusCode().value(),
                        URI.create(url), auditLogId);
                }
            }
        } catch (Exception ex) {
            log.error("Unexpected error occurred during audit Feign call: {}", ex.getMessage(), ex);
        }
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
