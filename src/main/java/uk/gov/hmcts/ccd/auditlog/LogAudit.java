package uk.gov.hmcts.ccd.auditlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on the endpoint method to create the audit log entry and send to stdout.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface LogAudit {

    AuditOperationType operationType();

    String caseId() default "";

    String jurisdiction() default "";

    String caseType() default "";

    String eventName() default "";

    String targetIdamId() default "";

    // for search cases
    String caseTypeIds() default "";

    String targetCaseRoles() default "";

}
