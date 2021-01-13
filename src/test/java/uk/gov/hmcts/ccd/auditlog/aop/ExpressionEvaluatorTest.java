package uk.gov.hmcts.ccd.auditlog.aop;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseRoleDefinition;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExpressionEvaluatorTest {

    private ExpressionEvaluator evaluator;

    @Before
    public void setup() {
        evaluator = new ExpressionEvaluator();
    }

    @Test
    public void shouldCreateEvaluationContext() {

        Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);
        EvaluationContext context = evaluator.createEvaluationContext(this, SampleMethods.class, method,
            new Object[] {"test", true});

        assertThat(context.lookupVariable("a0")).isEqualTo("test");
        assertThat(context.lookupVariable("p0")).isEqualTo("test");
        assertThat(context.lookupVariable("foo")).isEqualTo("test");

        assertThat(context.lookupVariable("a1")).isEqualTo(true);
        assertThat(context.lookupVariable("p1")).isEqualTo(true);
        assertThat(context.lookupVariable("flag")).isEqualTo(true);

        assertThat(context.lookupVariable("a2")).isNull();
        assertThat(context.lookupVariable("p2")).isNull();
    }

    @Test
    public void shouldParseValidExpressions() {

        Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);
        EvaluationContext context = evaluator.createEvaluationContext(this, SampleMethods.class, method,
            new Object[] {"test", true});
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, SampleMethods.class);

        assertThat(evaluator.condition("#foo", elementKey, context, String.class)).isEqualTo("test");
        assertThat(evaluator.condition("#flag", elementKey, context, Boolean.class)).isEqualTo(true);
        assertThat(evaluator.condition("#unknownProperty", elementKey, context, String.class)).isNull();
    }

    @Test
    public void shouldParseBeanExpressions() {
        Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class,
            CaseRoleDefinition.class);
        CaseRoleDefinition caseRole = new CaseRoleDefinition();
        caseRole.setName("citizen");
        EvaluationContext context = evaluator.createEvaluationContext(this, SampleMethods.class, method,
            new Object[] {"test", caseRole});
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, SampleMethods.class);

        assertThat(evaluator.condition("#caseRole.name", elementKey, context,
            String.class)).isEqualTo("citizen");
    }


    @Test
    public void shouldThrowErrorWhenPropertyNotFound() {

        Method method =
            ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, CaseRoleDefinition.class);
        CaseRoleDefinition caseRole = new CaseRoleDefinition();
        caseRole.setName("citizen");
        EvaluationContext context =
            evaluator.createEvaluationContext(this, SampleMethods.class, method, new Object[] {"test", caseRole});
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, SampleMethods.class);

        Exception exception = assertThrows(SpelEvaluationException.class, () -> {
            evaluator.condition("#caseRole.unknownProperty", elementKey, context, String.class);
        });

        assertThat(exception.getMessage().contains("EL1008E: Property or field 'unknownProperty' cannot be found"));

    }

    @SuppressWarnings("unused")
    private static class SampleMethods {

        private void hello(String foo, Boolean flag) {
        }

        private void hello(String foo, CaseRoleDefinition caseRole) {
        }
    }
}
