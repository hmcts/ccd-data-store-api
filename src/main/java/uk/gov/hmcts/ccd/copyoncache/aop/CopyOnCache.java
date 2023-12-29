package uk.gov.hmcts.ccd.copyoncache.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CopyOnCache {
    String cacheName();
    String key() default "";
    boolean copy();
    String unless() default "";
    String condition() default "";
}
