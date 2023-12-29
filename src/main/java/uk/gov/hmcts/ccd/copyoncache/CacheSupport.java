package uk.gov.hmcts.ccd.copyoncache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.copyoncache.aop.CacheExpressionEvaluator;
import uk.gov.hmcts.ccd.copyoncache.aop.CacheMetadata;

@Slf4j
@Component
public class CacheSupport extends AbstractCacheInvoker {

    private final KeyGenerator keyGenerator = new SimpleKeyGenerator();
    private final CacheExpressionEvaluator cacheExpressionEvaluator;
    private final CacheManager cacheManager;

    public CacheSupport(CacheExpressionEvaluator cacheExpressionEvaluator, CacheManager cacheManager) {
        this.cacheExpressionEvaluator = cacheExpressionEvaluator;
        this.cacheManager = cacheManager;
    }

    @Nullable
    public Object execute(final CacheOperationInvoker cacheOperationInvoker, final CacheMetadata cacheMetadata) {
        Cache cache = retrieveCache(cacheMetadata.getCacheName(), cacheMetadata.getMethod().getName());
        return execute(cacheOperationInvoker, new CacheOperationContext(cacheMetadata, cache));
    }

    private Object execute(final CacheOperationInvoker cacheOperationInvoker, final CacheOperationContext context) {
        Object cached = findInCache(context);
        if (cached != null) {
            return cached;
        }

        Object result = cacheOperationInvoker.invoke();
        putCache(context, result);

        return result;
    }

    private Cache retrieveCache(String cacheName, String operationName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Cannot find cache named '" + cacheName
                + "' for operation '" + operationName + "'");
        }
        return cache;
    }

    private Object findInCache(CacheOperationContext context) {
        Object noResult = CacheExpressionEvaluator.NO_RESULT;
        if (context.isConditionPassing(noResult)) {
            Object cacheKey = context.generateKey(noResult);
            Cache.ValueWrapper wrapper = doGet(context.getCache(), cacheKey);

            if (wrapper != null) {
                log.debug("Cache entry for key '{}' found in cache '{}'", cacheKey, context.getCache().getName());
                return Helper.wrapValueWithOptional(context.cacheMetadata.getMethod(), wrapper.get());
            }
        }

        return null;
    }

    private void putCache(CacheOperationContext context, @Nullable Object result) {
        Object cacheValue = Helper.unwrapValueFromOptional(result);
        Object noResult = CacheExpressionEvaluator.NO_RESULT;
        if (context.isConditionPassing(noResult) && context.isCacheable(cacheValue)) {
            Object key = context.generateKey(noResult);
            doPut(context.getCache(), key, cacheValue);
        }
    }

    @Getter
    protected class CacheOperationContext {
        private final CacheMetadata cacheMetadata;
        private final Cache cache;
        @Nullable
        private Object cacheKey;
        @Nullable
        private Boolean conditionPassing;

        public CacheOperationContext(CacheMetadata cacheMetadata, Cache cache) {
            this.cacheMetadata = cacheMetadata;
            this.cache = cache;
        }

        protected boolean isConditionPassing(@Nullable Object result) {
            if (conditionPassing == null) {
                if (StringUtils.hasText(cacheMetadata.getCondition())) {
                    EvaluationContext evaluationContext = createEvaluationContext(result);
                    conditionPassing = cacheExpressionEvaluator.condition(cacheMetadata.getCondition(),
                        cacheMetadata.getMethodKey(), evaluationContext);
                } else {
                    conditionPassing = true;
                }
            }

            return conditionPassing;
        }

        protected Object generateKey(@Nullable Object result) {
            if (cacheKey == null) {
                if (StringUtils.hasText(cacheMetadata.getKey())) {
                    EvaluationContext evaluationContext =
                        cacheExpressionEvaluator.createEvaluationContext(cacheMetadata,
                        result);
                    cacheKey = cacheExpressionEvaluator.key(cacheMetadata.getKey(), cacheMetadata.getMethodKey(),
                        evaluationContext);
                } else {
                    cacheKey = keyGenerator.generate(cacheMetadata.getTarget(), cacheMetadata.getMethod(),
                        cacheMetadata.getArgs());
                }
            }

            return cacheKey;
        }

        protected boolean isCacheable(@Nullable Object result) {
            if (StringUtils.hasText(cacheMetadata.getUnless())) {
                EvaluationContext evaluationContext = createEvaluationContext(result);
                return !cacheExpressionEvaluator.unless(cacheMetadata.getUnless(), cacheMetadata.getMethodKey(),
                    evaluationContext);
            }

            return true;
        }

        private EvaluationContext createEvaluationContext(Object result) {
            return cacheExpressionEvaluator.createEvaluationContext(cacheMetadata, result);
        }
    }
}
