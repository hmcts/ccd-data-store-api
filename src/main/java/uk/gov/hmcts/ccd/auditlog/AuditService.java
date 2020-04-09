package uk.gov.hmcts.ccd.auditlog;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
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

    public String prepareAuditMessage(HttpServletRequest request, HttpServletResponse response, String operationType) {
        LogMessage log = new LogMessage();

        String formattedDate = LocalDateTime.now(clock).format(ISO_LOCAL_DATE_TIME);

        log.setDateTime(formattedDate);
        log.setHttpStatus(response.getStatus());
        log.setHttpMethod(request.getMethod());
        log.setPath(request.getRequestURI());
        log.setClientIp(request.getRemoteAddr());
        log.setOperationType(operationType);
        log.setResponse(getResponsePayload(response));
        log.setIdamId(userRepository.getUser().getEmail());
        log.setInvokingService(securityUtils.getServiceName());
        return log.toString();
//        if ("POST".equalsIgnoreCase(request.getMethod())) {
//            logger.info("***** POST *****");
//            logger.info(getRequestPayload(request));
//        } else {
//            logger.info("***** REQUEST *****");
//            logger.info(log);
//        }
    }

    private String getRequestPayload(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {

            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, 5120);
                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                }
                catch (UnsupportedEncodingException ex) {
                    // NOOP
                }
            }
        }
        return "";
    }

    private String getResponsePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {

            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, 5120);
                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                }
                catch (UnsupportedEncodingException ex) {
                    // NOOP
                }
            }
        }
        return "";
    }
}
