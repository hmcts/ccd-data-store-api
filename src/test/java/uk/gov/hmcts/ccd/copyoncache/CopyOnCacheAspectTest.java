package uk.gov.hmcts.ccd.copyoncache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CopyOnCacheAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private CopyOnCacheAspect copyOnCacheAspect;

    AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void testCreateCopyAdviceWithCopyable() throws Throwable {
        TestCopyable mockCopyable = mock(TestCopyable.class);
        TestCopyable mockClonedCopyable = mock(TestCopyable.class);
        when(mockCopyable.createCopy()).thenReturn(mockClonedCopyable);
        when(joinPoint.proceed()).thenReturn(mockCopyable);
        Object result = copyOnCacheAspect.createCopyAdvice(joinPoint, null);

        assertAll(
            () -> assertNotNull(result),
            () -> assertTrue(result instanceof TestCopyable)
        );

        verify(joinPoint, times(1)).proceed();
        verify(mockCopyable, times(1)).createCopy();
    }

    @Test
    void testCreateCopyAdviceWithCopyable_shouldReturnNull() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);
        Object result = copyOnCacheAspect.createCopyAdvice(joinPoint, null);

        assertAll(
            () -> assertNull(result),
            () -> assertFalse(result instanceof TestCopyable)
        );

        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void createCopyAdvice_listCopyable_shouldReturnClonedList() throws Throwable {
        TestCopyable mockCopyable = mock(TestCopyable.class);
        TestCopyable mockClonedCopyable = mock(TestCopyable.class);

        List<Copyable<TestCopyable>> mockList = new ArrayList<>();
        mockList.add(mockCopyable);

        when(mockCopyable.createCopy()).thenReturn(mockClonedCopyable);
        when(joinPoint.proceed()).thenReturn(mockList);

        Object result = copyOnCacheAspect.createCopyAdvice(joinPoint, null);

        assertAll(
            () -> assertNotNull(result),
            () -> {
                assert result instanceof List<?>;
                List<?> clonedList = (List<?>) result;
                assertEquals(1, clonedList.size());
            }
        );

        verify(joinPoint, times(1)).proceed();
        verify(mockCopyable).createCopy();
    }

    @Test
    void createCopyAdvice_listCopyable_shouldReturnNull() throws Throwable {
        TestCopyable mockCopyable = mock(TestCopyable.class);
        TestCopyable mockClonedCopyable = mock(TestCopyable.class);

        List<Copyable<TestCopyable>> mockList = new ArrayList<>();
        mockList.add(mockCopyable);
        mockList.add(null);

        when(mockCopyable.createCopy()).thenReturn(mockClonedCopyable);
        when(joinPoint.proceed()).thenReturn(mockList);

        Object result = copyOnCacheAspect.createCopyAdvice(joinPoint, null);

        assertAll(
            () -> assertNotNull(result),
            () -> {
                assert result instanceof List<?>;
                List<?> clonedList = (List<?>) result;
                assertEquals(2, clonedList.size());
            }
        );

        verify(joinPoint, times(1)).proceed();
        verify(mockCopyable).createCopy();
    }

    @Test
    void createCopyAdvice_notCopyable_shouldThrowException() throws Throwable {
        when(joinPoint.proceed()).thenReturn(new Object());

        assertThrows(UnsupportedOperationException.class,
            () -> copyOnCacheAspect.createCopyAdvice(joinPoint, null));
    }

    private static class TestCopyable implements Copyable<TestCopyable> {

        @Override
        public TestCopyable createCopy() {
            return new TestCopyable();
        }
    }
}
