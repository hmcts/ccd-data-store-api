package uk.gov.hmcts.ccd.auditlog.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.auditlog.LogAudit;

import java.lang.reflect.Method;


@Aspect
@Component
public class AuditAspect {

    private static final String RESULT_VARIABLE = "result";

    private ExpressionEvaluator<Object> evaluator = new ExpressionEvaluator<>();

    @Around("@annotation(logAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, LogAudit logAudit) throws Throwable {
        Object result;
        String caseId = null;
        String jurisdiction = null;
        try {
            result = joinPoint.proceed();
            caseId = (String) getValue(joinPoint, joinPoint.getArgs(), result, logAudit.caseId());
            jurisdiction = (String) getValue(joinPoint, joinPoint.getArgs(), result, logAudit.jurisdiction());
        } catch (Exception ex) {
            throw ex;
        } finally {
            AuditContextHolder.setAuditContext(AuditContext.auditContextWith()
                .operationType(logAudit.operationType())
                .caseId(caseId)
                .jurisdiction(jurisdiction)
                .build());
        }
        return result;
    }

    private Object getValue(JoinPoint joinPoint, Object[] args, Object result, String condition) {
        if (condition.contains(RESULT_VARIABLE)) {
            return getValueFromResult(joinPoint, result, condition);
        }
        return getValueFromArgs(joinPoint, args, condition);
    }

    private Object getValueFromResult(JoinPoint joinPoint, Object result, String condition) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getThis(),
            joinPoint.getThis().getClass(), method, joinPoint.getArgs());
        evaluationContext.setVariable(RESULT_VARIABLE, result);
        return getValue(evaluationContext, joinPoint.getThis().getClass(), method, condition);
    }

    private Object getValueFromArgs(JoinPoint joinPoint, Object[] args, String condition) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getTarget(),
            joinPoint.getTarget().getClass(), method, args);
        return getValue(evaluationContext, joinPoint.getTarget().getClass(), method, condition);
    }

    private Object getValue(EvaluationContext evaluationContext, Class clazz, Method method, String condition) {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, clazz);
        return evaluator.condition(condition, methodKey, evaluationContext, Object.class);
    }
}
