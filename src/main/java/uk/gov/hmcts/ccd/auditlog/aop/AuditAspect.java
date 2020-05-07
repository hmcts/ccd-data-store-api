package uk.gov.hmcts.ccd.auditlog.aop;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.auditlog.LogAudit;

import java.lang.reflect.Method;
import java.util.List;

@Aspect
@Component
@ConditionalOnProperty(name = "audit.log.enabled", havingValue = "true")
public class AuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AuditAspect.class);

    private static final String RESULT_VARIABLE = "result";

    private ExpressionEvaluator evaluator = new ExpressionEvaluator();

    @Around("@annotation(logAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, LogAudit logAudit) throws Throwable {
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            String caseId =  getValue(joinPoint, logAudit.caseId(), result, String.class);
            String caseType =  getValue(joinPoint, logAudit.caseType(), result, String.class);
            String jurisdiction =  getValue(joinPoint, logAudit.jurisdiction(), result, String.class);
            String eventName =  getValue(joinPoint, logAudit.eventName(), result, String.class);
            String targetIdamId =  getValue(joinPoint, logAudit.targetIdamId(), result, String.class);
            String caseTypeIds =  getValue(joinPoint, logAudit.caseTypeIds(), result, String.class);
            List<String> targetCaseRoles =  getValue(joinPoint, logAudit.targetCaseRoles(), result, List.class);

            AuditContextHolder.setAuditContext(AuditContext.auditContextWith()
                .auditOperationType(logAudit.operationType())
                .caseId(caseId)
                .caseType(caseType)
                .jurisdiction(jurisdiction)
                .eventName(eventName)
                .targetIdamId(targetIdamId)
                .caseTypeIds(caseTypeIds)
                .targetCaseRoles(targetCaseRoles)
                .build());
        }
    }

    private <T> T getValue(JoinPoint joinPoint, String condition, Object result, Class<T> returnType) {
        if (StringUtils.isNotBlank(condition) && !(result == null && condition.contains(RESULT_VARIABLE))) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            try {
                EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getThis(),
                    joinPoint.getThis().getClass(), method, joinPoint.getArgs());
                evaluationContext.setVariable(RESULT_VARIABLE, result);
                AnnotatedElementKey methodKey = new AnnotatedElementKey(method, joinPoint.getThis().getClass());
                return evaluator.condition(condition, methodKey, evaluationContext, returnType);
            } catch (SpelEvaluationException ex) {
                LOG.warn("Error evaluating LogAudit annotation expression:{} on method:{}", condition, method.getName(), ex);
                return null;
            }
        }
        return null;
    }
}
