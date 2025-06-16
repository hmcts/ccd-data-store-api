package uk.gov.hmcts.ccd.domain.service.lau;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.AuditCaseRemoteConfiguration;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAndAuditFeignClient;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.lau.ActionLog;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.SearchLog;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

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

    private final LogAndAuditFeignClient logAndAuditFeignClient;

    private final SecurityUtils securityUtils;

    private final AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    @Autowired
    public AuditCaseRemoteOperation(@Lazy final SecurityUtils securityUtils,
                                    LogAndAuditFeignClient logAndAuditFeignClient,
                                    final AuditCaseRemoteConfiguration auditCaseRemoteConfiguration) {
        this.securityUtils = securityUtils;
        this.logAndAuditFeignClient = logAndAuditFeignClient;
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

                postAsyncAuditRequestAndHandleResponse(entry, activity, capr,null, lauCaseAuditUrl);

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

                postAsyncAuditRequestAndHandleResponse(entry, activity,null, cspr, lauCaseAuditUrl);

            } else {
                log.warn("The operational type " + entry.getOperationType()
                    + " is not recognised as a valid case search.");
            }

        } catch (Exception excep) {
            log.error("Error occurred while generating remote log and audit search request.", excep);
        }
    }

    @Async
    private void postAsyncAuditRequestAndHandleResponse(AuditEntry entry, String activity,
             CaseActionPostRequest capr, CaseSearchPostRequest cspr,String url) {
        try {
            String auditLogId = UUID.randomUUID().toString();

            logCorrelationId(entry.getRequestId(), activity, entry.getJurisdiction(), entry.getIdamId(), auditLogId);
            Response response = null;
            // Use FeignClient to send the request
            try {
                if (activity.equals("CREATE") || activity.equals("UPDATE") || activity.equals("VIEW")) {
                    response = logAndAuditFeignClient.postCaseAction(securityUtils.getServiceAuthorization(), capr);
                } else if (activity.equals("SEARCH")) {
                    response = logAndAuditFeignClient.postCaseSearch(securityUtils.getServiceAuthorization(), cspr);
                }
                if(response != null){
                    logAuditResponse(entry.getRequestId(), activity, response.status(), URI.create(url), auditLogId);
                }
            } catch (final Exception ex){
                log.error("Error occurred while processing response for remote log and audit request.", ex);
            }
        } catch (Exception ex) {
            log.error("Error occurred while processing response for remote log and audit request.", ex);
        }
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
