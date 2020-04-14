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
import uk.gov.hmcts.ccd.auditlog.LogMessage;

import java.lang.reflect.Method;


@Aspect
@Component
public class AuditAspect {

    private static final String RESULT_VARIABLE = "result";

    private ExpressionEvaluator<Object> evaluator = new ExpressionEvaluator<>();

    @Around("@annotation(logAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, LogAudit logAudit) throws Throwable {
        Object result = joinPoint.proceed();
        // TODO : try lookup in the result Object based on expression string (contains "result") otherwise from args.
        // introduce try/catch/finally for better handling of error scenarios.
        String caseId = (String) getValueFromArgs(joinPoint, logAudit.caseId());
        String jurisdiction = (String) getValueFromResult(joinPoint, result, logAudit.jurisdiction());
        storeInAuditContext(caseId, jurisdiction);
        return result;
    }

    private Object getValueFromResult(JoinPoint joinPoint, Object result, String condition) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getThis(),
            joinPoint.getThis().getClass(), method, joinPoint.getArgs());
        evaluationContext.setVariable(RESULT_VARIABLE, result);
        return getValue(evaluationContext, joinPoint.getThis().getClass(), method, condition);
    }

    private Object getValueFromArgs(JoinPoint joinPoint, String condition) {
        Object[] args = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getTarget(),
            joinPoint.getTarget().getClass(), method, args);
        return getValue(evaluationContext, joinPoint.getTarget().getClass(), method, condition);
    }

    private Object getValue(EvaluationContext evaluationContext, Class clazz, Method method, String condition) {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, clazz);
        return evaluator.condition(condition, methodKey, evaluationContext, Object.class);
    }

    private void storeInAuditContext(String caseId, String jurisdiction) {
        LogMessage logMessage = new LogMessage();
        logMessage.setCaseId(caseId);
        logMessage.setJurisdiction(jurisdiction);
        AuditContextHolder.setAuditContext(logMessage);
    }

}
