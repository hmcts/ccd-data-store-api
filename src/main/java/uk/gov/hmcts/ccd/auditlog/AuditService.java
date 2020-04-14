package uk.gov.hmcts.ccd.auditlog;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
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

    public void audit(HttpServletRequest request, int httpResponseStatus, AuditContext auditContext) {
        AuditEntry entry = new AuditEntry();

        String formattedDate = LocalDateTime.now(clock).format(ISO_LOCAL_DATE_TIME);

        entry.setDateTime(formattedDate);
        entry.setHttpStatus(httpResponseStatus);
        entry.setHttpMethod(request.getMethod());
        entry.setPath(request.getRequestURI());
        entry.setClientIp(request.getRemoteAddr());
        entry.setIdamId(userRepository.getUser().getEmail());
        entry.setInvokingService(securityUtils.getServiceName());

        entry.setOperationType(auditContext.getOperationType().getLabel());
        entry.setJurisdiction(auditContext.getJurisdiction());
        entry.setCaseId(auditContext.getCaseId());
        entry.setCaseType(auditContext.getCaseType());

        auditRepository.save(entry);
    }

}
