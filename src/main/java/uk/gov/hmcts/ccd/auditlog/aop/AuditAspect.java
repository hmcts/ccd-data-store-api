package uk.gov.hmcts.ccd.auditlog.aop;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.auditlog.LogAudit;

import java.lang.reflect.Method;


@Aspect
@Component
public class AuditAspect {

    private static final String RESULT_VARIABLE = "result";

    private ExpressionEvaluator evaluator = new ExpressionEvaluator();

    @Around("@annotation(logAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, LogAudit logAudit) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception ex) {
            buildAuditContextFromArgsOnly(joinPoint, logAudit);
            throw ex;
        }
        String caseId =  getValue(joinPoint, logAudit.caseId(), result, String.class);
        String caseType =  getValue(joinPoint, logAudit.caseType(), result, String.class);
        String jurisdiction =  getValue(joinPoint, logAudit.jurisdiction(), result, String.class);

        setAuditContext(logAudit, caseId, caseType, jurisdiction);
        return result;
    }

    private void setAuditContext(LogAudit logAudit, String caseId, String caseType, String jurisdiction) {
        AuditContextHolder.setAuditContext(AuditContext.auditContextWith()
            .operationType(logAudit.operationType())
            .caseId(caseId)
            .caseType(caseType)
            .jurisdiction(jurisdiction)
            .build());
    }

    private <T> T tryGetFromArgsOnly(JoinPoint joinPoint, String condition, Class<T> returnType) {
        try {
            return getValue(joinPoint, condition, null, returnType);
        } catch (SpelEvaluationException ex) {
            return null;
        }
    }

    private <T> T getValue(JoinPoint joinPoint, String condition, Object result, Class<T> returnType) {
        if (StringUtils.isNotBlank(condition)) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getThis(),
                joinPoint.getThis().getClass(), method, joinPoint.getArgs());
            evaluationContext.setVariable(RESULT_VARIABLE, result);
            AnnotatedElementKey methodKey = new AnnotatedElementKey(method, joinPoint.getThis().getClass());
            return evaluator.condition(condition, methodKey, evaluationContext, returnType);
        }
        return null;
    }

    private void buildAuditContextFromArgsOnly(ProceedingJoinPoint joinPoint, LogAudit logAudit) {
        String caseId =  tryGetFromArgsOnly(joinPoint, logAudit.caseId(), String.class);
        String caseType =  tryGetFromArgsOnly(joinPoint, logAudit.caseType(), String.class);
        String jurisdiction =  tryGetFromArgsOnly(joinPoint, logAudit.jurisdiction(), String.class);
        setAuditContext(logAudit, caseId, caseType, jurisdiction);
    }
}
