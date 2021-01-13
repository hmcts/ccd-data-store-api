package uk.gov.hmcts.ccd.auditlog;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Service
public class AuditService {

    private final Clock clock;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;

    public AuditService(@Qualifier("utcClock") final Clock clock,
                        @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                        @Lazy final SecurityUtils securityUtils, final AuditRepository auditRepository) {
        this.clock = clock;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.auditRepository = auditRepository;
    }

    public void audit(AuditContext auditContext) {
        AuditEntry entry = new AuditEntry();

        String formattedDate = LocalDateTime.now(clock).format(ISO_LOCAL_DATE_TIME);
        entry.setDateTime(formattedDate);

        entry.setHttpStatus(auditContext.getHttpStatus());
        entry.setHttpMethod(auditContext.getHttpMethod());
        entry.setPath(auditContext.getRequestPath());
        entry.setRequestId(auditContext.getRequestId());
        entry.setIdamId(userRepository.getUser().getId());
        entry.setInvokingService(securityUtils.getServiceName());

        entry.setOperationType(auditContext.getAuditOperationType() != null
            ? auditContext.getAuditOperationType().getLabel() : null);
        entry.setJurisdiction(auditContext.getJurisdiction());
        entry.setCaseId(auditContext.getCaseId());
        entry.setCaseType(auditContext.getCaseType());
        entry.setListOfCaseTypes(auditContext.getCaseTypeIds());
        entry.setEventSelected(auditContext.getEventName());
        entry.setTargetIdamId(auditContext.getTargetIdamId());
        entry.setTargetCaseRoles(auditContext.getTargetCaseRoles());

        auditRepository.save(entry);
    }

}
