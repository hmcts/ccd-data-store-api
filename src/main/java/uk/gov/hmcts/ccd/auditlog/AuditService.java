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

    public AuditService(@Qualifier("utcClock") final Clock clock,
                        @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                        @Lazy final SecurityUtils securityUtils) {
        this.clock = clock;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    public String prepareAuditMessage(HttpServletRequest request, int httpResponseStatus, AuditContext auditContext) {
        LogMessage log = new LogMessage();

        String formattedDate = LocalDateTime.now(clock).format(ISO_LOCAL_DATE_TIME);

        log.setDateTime(formattedDate);
        log.setHttpStatus(httpResponseStatus);
        log.setHttpMethod(request.getMethod());
        log.setPath(request.getRequestURI());
        log.setClientIp(request.getRemoteAddr());
        log.setIdamId(userRepository.getUser().getEmail());
        log.setInvokingService(securityUtils.getServiceName());

        log.setOperationType(auditContext.getOperationType().getLabel());
        log.setJurisdiction(auditContext.getJurisdiction());
        log.setCaseId(auditContext.getCaseId());
        return log.toString();
    }

}
