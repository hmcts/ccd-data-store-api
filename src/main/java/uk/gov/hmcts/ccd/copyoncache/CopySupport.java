package uk.gov.hmcts.ccd.copyoncache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CopySupport {

    public Object createCopy(Method method, Object returnValue) {
        Object unwrappedReturnValue = Helper.unwrapValueFromOptional(returnValue);
        if (unwrappedReturnValue == null) {
            return null;
        }

        if (isCopyable(unwrappedReturnValue)) {
            return Helper.wrapValueWithOptional(method, cloneSingleCopyable((Copyable<?>) unwrappedReturnValue));
        }

        if (unwrappedReturnValue instanceof List<?> list) {
            boolean isListCopyable = list.stream()
                .anyMatch(this::isCopyable);

            if (isListCopyable) {
                return Helper.wrapValueWithOptional(method, cloneListCopyable(list));
            }
        }

        throw new UnsupportedOperationException("createCopy() operation is not supported by '"
            + unwrappedReturnValue.getClass() + "'");

    }

    private Object cloneSingleCopyable(Copyable<?> copyable) {
        Object clonedObject = copyable.createCopy();
        if (log.isDebugEnabled()) {
            log.debug("Cached object hashCode:{}, Cloned object hashCode:{}", copyable.hashCode(),
                clonedObject.hashCode());
        }

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
