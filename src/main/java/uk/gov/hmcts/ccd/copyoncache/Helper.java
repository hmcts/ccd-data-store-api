package uk.gov.hmcts.ccd.copyoncache;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Optional;

public class Helper {

    static Object wrapValueWithOptional(Method method, @Nullable Object value) {
        if (method.getReturnType() == Optional.class
            && (value == null || value.getClass() != Optional.class)) {
            return Optional.ofNullable(value);
        }
        return value;
    }

    @Nullable
    static Object unwrapValueFromOptional(@Nullable Object value) {
        return ObjectUtils.unwrapOptional(value);
    }
}
