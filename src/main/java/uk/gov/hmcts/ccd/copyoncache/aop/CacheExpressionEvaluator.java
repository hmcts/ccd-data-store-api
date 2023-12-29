package uk.gov.hmcts.ccd.copyoncache.aop;

import lombok.Getter;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheExpressionEvaluator extends CachedExpressionEvaluator {
    private static final String RESULT_VARIABLE = "result";
    public static final Object NO_RESULT = new Object();
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);
    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);
    private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<>(64);
    private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>(64);


    public EvaluationContext createEvaluationContext(CacheMetadata cacheMetadata, @Nullable Object result) {
        EvaluationContext evaluationContext = createEvaluationContext(cacheMetadata.getJointObject(),
            cacheMetadata.getJointObject().getClass(), cacheMetadata.getMethod(), cacheMetadata.getArgs(),
            cacheMetadata.getMethodKey());

        if (result != NO_RESULT) {
            evaluationContext.setVariable(RESULT_VARIABLE, result);
        }

        return evaluationContext;
    }

    private EvaluationContext createEvaluationContext(Object object,
                                                      Class<?> targetClass,
                                                      Method method, Object[] args, AnnotatedElementKey methodKey) {
        Method targetMethod = getTargetMethod(targetClass, method, methodKey);
        RootObject root = new RootObject(object, args);
        return new MethodBasedEvaluationContext(root, targetMethod, args, this.paramNameDiscoverer);
    }

    @Nullable
    public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
    }

    public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
            evalContext, Boolean.class)));
    }

    public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return (Boolean.TRUE.equals(getExpression(this.unlessCache, methodKey, unlessExpression).getValue(
            evalContext, Boolean.class)));
    }

    private Method getTargetMethod(Class<?> targetClass, Method method, AnnotatedElementKey methodKey) {
        Method targetMethod = this.targetMethodCache.get(methodKey);
        if (targetMethod == null) {
            targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            this.targetMethodCache.put(methodKey, targetMethod);
        }
        return targetMethod;
    }

    @Getter
    private static class RootObject {
        private final Object object;
        private final Object[] args;

        public RootObject(Object object, Object[] args) {
            this.object = object;
            this.args = args;
        }
    }
}
