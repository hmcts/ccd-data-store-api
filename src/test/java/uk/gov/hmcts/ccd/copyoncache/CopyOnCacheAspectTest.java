package uk.gov.hmcts.ccd.copyoncache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import uk.gov.hmcts.ccd.copyoncache.aop.CopyOnCache;
import uk.gov.hmcts.ccd.copyoncache.aop.CopyOnCacheAspect;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CopyOnCacheAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    MethodSignature mockMethodSignature;

    @Mock
    CopyOnCache copyOnCache;

    @Mock
    CacheSupport cacheSupport;

    @Mock
    CopySupport copySupport;

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

        when(copyOnCache.copy()).thenReturn(true);
        when(copyOnCache.key()).thenReturn("key");

        when(joinPoint.getSignature()).thenReturn(mockMethodSignature);
        when(joinPoint.getThis()).thenReturn(mockCopyable);
        Method mockMethod = TestCopyable.class.getDeclaredMethod("createCopy");
        when(mockMethodSignature.getMethod()).thenReturn(mockMethod);

        when(copySupport.createCopy(any(), any())).thenReturn(mockCopyable);
        when(cacheSupport.execute(any(), any())).thenReturn(mockCopyable);

        Object result = copyOnCacheAspect.processCopyOnCache(joinPoint, copyOnCache);

        assertAll(
            () -> assertNotNull(result),
            () -> assertInstanceOf(TestCopyable.class, result)
        );

        verify(copySupport, times(1)).createCopy(any(), any());
        verify(cacheSupport, times(1)).execute(any(CacheOperationInvoker.class), any());
    }

    @Test
    void testCreateCopyAdviceWithoutCopyable() throws Throwable {
        TestCopyable mockCopyable = mock(TestCopyable.class);

        when(copyOnCache.copy()).thenReturn(false);
        when(copyOnCache.key()).thenReturn("key");

        when(joinPoint.getSignature()).thenReturn(mockMethodSignature);
        when(joinPoint.getThis()).thenReturn(mockCopyable);
        Method mockMethod = TestCopyable.class.getDeclaredMethod("createCopy");
        when(mockMethodSignature.getMethod()).thenReturn(mockMethod);

        when(copySupport.createCopy(any(), any())).thenReturn(mockCopyable);
        when(cacheSupport.execute(any(), any())).thenReturn(mockCopyable);

        Object result = copyOnCacheAspect.processCopyOnCache(joinPoint, copyOnCache);

        assertAll(
            () -> assertNotNull(result),
            () -> assertInstanceOf(TestCopyable.class, result)
        );

        verify(copySupport, never()).createCopy(any(), any());
    }

    @Test
    void createCopyAdvice_listCopyable_shouldReturnClonedList() throws Throwable {
        // Arrange
        TestCopyable mockCopyable = mock(TestCopyable.class);
        TestCopyable mockClonedCopyable = mock(TestCopyable.class);

        when(copyOnCache.copy()).thenReturn(true);
        when(copyOnCache.key()).thenReturn("key");

        when(joinPoint.getSignature()).thenReturn(mockMethodSignature);
        when(joinPoint.getThis()).thenReturn(mockCopyable);
        Method mockMethod = TestCopyable.class.getDeclaredMethod("createCopy");
        when(mockMethodSignature.getMethod()).thenReturn(mockMethod);

        List<TestCopyable> mockList = Arrays.asList(mockCopyable, mockCopyable);
        List<TestCopyable> mockClonedList = Arrays.asList(mockClonedCopyable, mockClonedCopyable);

        when(cacheSupport.execute(any(), any())).thenReturn(mockList);
        when(copySupport.createCopy(any(), any())).thenReturn(mockClonedList);

        Object result = copyOnCacheAspect.processCopyOnCache(joinPoint, copyOnCache);

        assertNotNull(result);
        assertInstanceOf(List.class, result);

        List<?> clonedList = (List<?>) result;
        assertEquals(mockList.size(), clonedList.size());

        for (int i = 0; i < mockList.size(); i++) {
            assertInstanceOf(TestCopyable.class, clonedList.get(i));
        }

        verify(copySupport, times(1)).createCopy(any(), any());
    }

    private static class TestCopyable implements Copyable<TestCopyable> {

        @Override
        public TestCopyable createCopy() {
            return new TestCopyable();
        }
    }
}
