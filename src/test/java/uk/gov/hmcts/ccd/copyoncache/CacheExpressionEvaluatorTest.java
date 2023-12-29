package uk.gov.hmcts.ccd.copyoncache;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.ccd.copyoncache.aop.CacheExpressionEvaluator;
import uk.gov.hmcts.ccd.copyoncache.aop.CacheMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseRoleDefinition;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CacheExpressionEvaluatorTest {
    private CacheExpressionEvaluator evaluator;

    @BeforeEach
    public void setup() {
        evaluator = new CacheExpressionEvaluator();
    }

    @Test
    public void shouldCreateEvaluationContext() {
        Method method = ReflectionUtils.findMethod(CacheExpressionEvaluatorTest.SampleMethods.class, "hello",
            String.class, Boolean.class);
        assert method != null;
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method,
            CacheExpressionEvaluatorTest.SampleMethods.class);

        CacheMetadata cacheMetadata = new CacheMetadata();
        cacheMetadata.setMethod(method);
        cacheMetadata.setTarget(CacheExpressionEvaluatorTest.SampleMethods.class);
        cacheMetadata.setArgs(new Object[]{"test", true});
        cacheMetadata.setMethodKey(elementKey);
        cacheMetadata.setJointObject(new Object());

        EvaluationContext context = evaluator.createEvaluationContext(cacheMetadata,
            CacheExpressionEvaluator.NO_RESULT);

        assertAll(
            () -> assertEquals(context.lookupVariable("a0"), "test"),
            () -> assertEquals(context.lookupVariable("p0"), "test"),
            () -> assertEquals(context.lookupVariable("foo"), "test"),

            () -> assertEquals(context.lookupVariable("a1"), true),
            () -> assertEquals(context.lookupVariable("p1"), true),
            () -> assertEquals(context.lookupVariable("flag"), true),

            () -> assertNull(context.lookupVariable("a2")),
            () -> assertNull(context.lookupVariable("p2")),
            () -> assertNull(context.lookupVariable("result"))
        );
    }

    @Test
    public void shouldParseValidConditionExpressions() {
        Method method = ReflectionUtils.findMethod(CacheExpressionEvaluatorTest.SampleMethods.class, "hello",
            String.class, Boolean.class);
        assert method != null;
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method,
            CacheExpressionEvaluatorTest.SampleMethods.class);
        CacheMetadata cacheMetadata = new CacheMetadata();
        cacheMetadata.setMethod(method);
        cacheMetadata.setTarget(CacheExpressionEvaluatorTest.SampleMethods.class);
        cacheMetadata.setArgs(new Object[]{"test", true});
        cacheMetadata.setMethodKey(elementKey);
        cacheMetadata.setJointObject(new Object());

        EvaluationContext context = evaluator.createEvaluationContext(cacheMetadata,
            CacheExpressionEvaluator.NO_RESULT);

        assertAll(
            () -> assertTrue(evaluator.condition("#flag", elementKey, context)),
            () -> assertFalse(evaluator.condition("#unknownProperty", elementKey, context))
        );
    }

    @Test
    public void shouldParseBeanConditionExpressions() {
        Method method = ReflectionUtils.findMethod(CacheExpressionEvaluatorTest.SampleMethods.class, "hello",
            String.class, Boolean.class);
        assert method != null;
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method,
            CacheExpressionEvaluatorTest.SampleMethods.class);
        CacheMetadata cacheMetadata = new CacheMetadata();
        cacheMetadata.setMethod(method);
        cacheMetadata.setTarget(CacheExpressionEvaluatorTest.SampleMethods.class);
        cacheMetadata.setArgs(new Object[]{"myFlag", true});
        cacheMetadata.setMethodKey(elementKey);
        cacheMetadata.setJointObject(new Object());

        EvaluationContext context = evaluator.createEvaluationContext(cacheMetadata,
            CacheExpressionEvaluator.NO_RESULT);

        assertTrue(evaluator.condition("#foo.equals('myFlag')", elementKey, context));
    }

    @Test
    public void shouldParseBeanUnlessExpressions() {
        Method method = ReflectionUtils.findMethod(CacheExpressionEvaluatorTest.SampleMethods.class, "hello",
            String.class, Boolean.class);
        assert method != null;
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method,
            CacheExpressionEvaluatorTest.SampleMethods.class);
        CacheMetadata cacheMetadata = new CacheMetadata();
        cacheMetadata.setMethod(method);
        cacheMetadata.setTarget(CacheExpressionEvaluatorTest.SampleMethods.class);
        cacheMetadata.setArgs(new Object[]{"myFlag", true});
        cacheMetadata.setMethodKey(elementKey);
        cacheMetadata.setJointObject(new Object());

        record Result(Object value) {}

        CaseRoleDefinition roleDefinition = new CaseRoleDefinition();
        roleDefinition.setName("test");
        EvaluationContext context = evaluator.createEvaluationContext(cacheMetadata,
            new Result(roleDefinition));

        assertAll(
            () -> assertEquals(context.lookupVariable("result"), new Result(roleDefinition)),
            () -> assertTrue(evaluator.unless("#result != null", elementKey, context))
        );
    }

    @Test
    public void shouldGenerateKeyExpressions() {
        Method method = ReflectionUtils.findMethod(CacheExpressionEvaluatorTest.SampleMethods.class, "hello",
            String.class, CaseRoleDefinition.class);
        assert method != null;
        final AnnotatedElementKey elementKey = new AnnotatedElementKey(method,
            CacheExpressionEvaluatorTest.SampleMethods.class);
        CaseRoleDefinition caseRole = new CaseRoleDefinition();
        caseRole.setName("citizen");
        CacheMetadata cacheMetadata = new CacheMetadata();
        cacheMetadata.setMethod(method);
        cacheMetadata.setTarget(CacheExpressionEvaluatorTest.SampleMethods.class);
        cacheMetadata.setArgs(new Object[]{"testKey", caseRole});
        cacheMetadata.setMethodKey(elementKey);
        cacheMetadata.setJointObject(new Object());
        EvaluationContext context = evaluator.createEvaluationContext(cacheMetadata,
            CacheExpressionEvaluator.NO_RESULT);

        assertAll(
            () -> assertNull(context.lookupVariable("result")),
            () -> assertEquals(evaluator.key("#foo.toUpperCase() +'_'+ #caseRole.getName().toUpperCase()",
                elementKey, context),"TESTKEY_CITIZEN")
        );
    }

    @Test
    public void shouldThrowErrorWhenPropertyNotFound() {
        Method method = ReflectionUtils.findMethod(CacheExpressionEvaluatorTest.SampleMethods.class, "hello",
            String.class, Boolean.class);
        assert method != null;
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method,
            CacheExpressionEvaluatorTest.SampleMethods.class);
        CacheMetadata cacheMetadata = new CacheMetadata();
        cacheMetadata.setMethod(method);
        cacheMetadata.setTarget(CacheExpressionEvaluatorTest.SampleMethods.class);
        cacheMetadata.setArgs(new Object[]{"myFlag", true});
        cacheMetadata.setMethodKey(elementKey);
        cacheMetadata.setJointObject(new Object());

        EvaluationContext context = evaluator.createEvaluationContext(cacheMetadata,
            CacheExpressionEvaluator.NO_RESULT);

        Exception exception = assertThrows(SpelEvaluationException.class, () ->
            evaluator.condition("#result.unknownProperty != null", elementKey, context));

        assertTrue(exception.getMessage().contains("EL1007E: Property or field 'unknownProperty' cannot be found"));
    }

    @SuppressWarnings("unused")
    private static class SampleMethods {

        private void hello(String foo, Boolean flag) {
        }

        private void hello(String foo, CaseRoleDefinition caseRole) {
        }
    }
}
