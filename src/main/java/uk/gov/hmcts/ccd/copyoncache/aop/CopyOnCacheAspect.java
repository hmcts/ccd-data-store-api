package uk.gov.hmcts.ccd.copyoncache.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.copyoncache.CacheSupport;
import uk.gov.hmcts.ccd.copyoncache.CopySupport;

import java.lang.reflect.Method;


@Slf4j
@Component
@Aspect
public class CopyOnCacheAspect {

    private final CopySupport copySupport;
    private final CacheSupport cacheSupport;

    public CopyOnCacheAspect(CopySupport copySupport, CacheSupport cacheSupport) {
        this.copySupport = copySupport;
        this.cacheSupport = cacheSupport;
    }

    @Around("@annotation(copyOnCache)")
    public Object processCopyOnCache(ProceedingJoinPoint joinPoint, CopyOnCache copyOnCache) {
        final CacheMetadata cacheMetadata = buildCacheContext(joinPoint, copyOnCache);
        Object cachedResult = retrieveFromCache(joinPoint, cacheMetadata);

        if (copyOnCache.copy()) {
            return copySupport.createCopy(cacheMetadata.getMethod(), cachedResult);
        }

        return cachedResult;
    }

    private Object retrieveFromCache(final ProceedingJoinPoint joinPoint, final CacheMetadata cacheMetadata) {
        CacheOperationInvoker cacheOperationInvoker = () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable ex) {
                throw new CacheOperationInvoker.ThrowableWrapper(ex);
            }
        };

        return cacheSupport.execute(cacheOperationInvoker, cacheMetadata);
    }

    private CacheMetadata buildCacheContext(ProceedingJoinPoint joinPoint, CopyOnCache copyOnCache) {
        final Method method  = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return CacheMetadata.builder()
            .key(copyOnCache.key())
            .cacheName(copyOnCache.cacheName())
            .unless(copyOnCache.unless())
            .condition(copyOnCache.condition())
            .method(method)
            .args(joinPoint.getArgs())
            .jointObject(joinPoint.getThis())
            .target(joinPoint.getTarget())
            .methodKey(new AnnotatedElementKey(method, joinPoint.getThis().getClass()))
            .build();
    }
}

