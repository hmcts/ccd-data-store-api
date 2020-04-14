package uk.gov.hmcts.ccd.auditlog.aop;

import uk.gov.hmcts.ccd.auditlog.LogMessage;

public class AuditContextHolder {

    private AuditContextHolder() {}

    private static final InheritableThreadLocal<LogMessage> threadLocal = new InheritableThreadLocal<>();


    public static void setAuditContext(LogMessage logMessage) {
        threadLocal.set(logMessage);
    }

    public static LogMessage getAuditContext() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
