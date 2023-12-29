package uk.gov.hmcts.ccd.copyoncache.aop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.expression.AnnotatedElementKey;

import java.lang.reflect.Method;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheMetadata {
    private String key;
    private String cacheName;
    private String unless;
    private String condition;
    private Method method;
    private Object[] args;
    private Object jointObject;
    private Object target;
    private AnnotatedElementKey methodKey;
}
