package uk.gov.hmcts.ccd.copyoncache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class CopyOnCacheAspect {

    @Around("@annotation(copyOnCacheAnnotation)")
    public Object createCopyAdvice(ProceedingJoinPoint joinPoint, CopyOnCache copyOnCacheAnnotation) throws Throwable {
        Object cachedObject = joinPoint.proceed();

        if (cachedObject == null) {
            return null;
        }

        if (isCopyable(cachedObject)) {
            return cloneSingleCopyable((Copyable<?>) cachedObject);
        }

        if (cachedObject instanceof List<?> list) {
            boolean isListCopyable = list.stream()
                .anyMatch(this::isCopyable);

            if (isListCopyable) {
                return cloneListCopyable(list);
            }
        }

        throw new UnsupportedOperationException("createCopy() operation is not supported by "
            + cachedObject.getClass());
    }

    private Object cloneSingleCopyable(Copyable<?> copyable) {
        Object clonedObject = copyable.createCopy();
        log.debug("Cached object hashCode:{}, Cloned object hashCode:{}", copyable.hashCode(),
            clonedObject.hashCode());
        return clonedObject;
    }

    private List<Object> cloneListCopyable(List<?> list) {
        return list.stream()
            .map(copyable -> Optional.ofNullable(copyable)
                .map(o -> cloneSingleCopyable((Copyable<?>) o))
                .orElse(null))
            .toList();
    }

    private boolean isCopyable(Object cachedObject) {
        return cachedObject instanceof Copyable;
    }
}
