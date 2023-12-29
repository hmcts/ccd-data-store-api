package uk.gov.hmcts.ccd.copyoncache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.SimpleKey;
import uk.gov.hmcts.ccd.copyoncache.aop.CacheExpressionEvaluator;
import uk.gov.hmcts.ccd.copyoncache.aop.CacheMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheSupportTest {

    public static final Object EMPTY_OBJECT = new Object();

    @Mock
    private CacheExpressionEvaluator cacheExpressionEvaluator;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private CacheSupport cacheSupport;

    @Test
    void testExecute() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});
        when(cacheManager.getCache("testCache")).thenReturn(cache);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cache, times(1)).put(any(), any());
    }

    @Test
    void testExecuteCachedValueReturned() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});
        when(cacheManager.getCache("testCache")).thenReturn(cache);

        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(cache.get(any())).thenReturn(wrapper);
        when(wrapper.get()).thenReturn(EMPTY_OBJECT);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        Object result = cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        assertEquals(EMPTY_OBJECT, result);
        verify(cacheOperationInvoker, never()).invoke();
        verify(cache, never()).put(any(), any());
    }

    @Test
    void testExecuteCacheNotFound() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheManager.getCache(any())).thenReturn(null);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        assertThrows(IllegalArgumentException.class, () -> cacheSupport.execute(cacheOperationInvoker, cacheMetadata));
    }

    @Test
    void testExecuteConditionPassingWhileConditionIsSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});
        when(cacheMetadata.getCondition()).thenReturn("testCondition");
        when(cacheManager.getCache("testCache")).thenReturn(cache);
        when(cacheExpressionEvaluator.condition(any(), any(), any())).thenReturn(true);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cacheExpressionEvaluator, times(1)).condition(any(), any(), any());
        verify(cache, times(1)).put(any(), any());
    }

    @Test
    void testExecuteConditionNotPassingWhileConditionIsSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getCondition()).thenReturn("testCondition");
        when(cacheManager.getCache("testCache")).thenReturn(cache);
        when(cacheExpressionEvaluator.condition(any(), any(), any())).thenReturn(false);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheExpressionEvaluator, times(1)).condition(any(), any(), any());
        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cache, never()).put(any(), any());
    }

    @Test
    void testExecuteConditionPassingWhileConditionIsNotSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});
        when(cacheManager.getCache("testCache")).thenReturn(cache);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheExpressionEvaluator, never()).condition(any(), any(), any());
        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cache, times(1)).put(any(), any());
    }

    @Test
    void testExecuteIsCacheableWhileUnlessIsSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});
        when(cacheMetadata.getUnless()).thenReturn("testUnless");

        when(cacheManager.getCache("testCache")).thenReturn(cache);
        when(cacheExpressionEvaluator.unless(any(), any(), any())).thenReturn(false);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheExpressionEvaluator, times(1)).unless(any(), any(), any());
        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cache, times(1)).put(any(), any());
    }

    @Test
    void testExecuteIsNotCacheableWhileUnlessIsSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});
        when(cacheMetadata.getUnless()).thenReturn("testUnless");

        when(cacheManager.getCache("testCache")).thenReturn(cache);
        when(cacheExpressionEvaluator.unless(any(), any(), any())).thenReturn(true);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheExpressionEvaluator, times(1)).unless(any(), any(), any());
        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cache, never()).put(any(), any());
    }

    @Test
    void testExecuteIsCacheableWhileUnlessIsNotSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);
        Cache cache = mock(Cache.class);

        when(cacheMetadata.getCacheName()).thenReturn("testCache");
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});

        when(cacheManager.getCache("testCache")).thenReturn(cache);

        CacheOperationInvoker cacheOperationInvoker = mock(CacheOperationInvoker.class);

        cacheSupport.execute(cacheOperationInvoker, cacheMetadata);

        verify(cacheExpressionEvaluator, never()).unless(any(), any(), any());
        verify(cacheOperationInvoker, times(1)).invoke();
        verify(cache, times(1)).put(any(), any());
    }

    @Test
    void testGenerateKeyWhileKeyIsSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);

        when(cacheMetadata.getKey()).thenReturn("customKey");

        when(cacheExpressionEvaluator.key(eq("customKey"), any(), any())).thenReturn("generatedKey");

        CacheSupport.CacheOperationContext context = cacheSupport.new CacheOperationContext(cacheMetadata, null);
        Object generatedKey = context.generateKey(EMPTY_OBJECT);

        verify(cacheExpressionEvaluator, times(1)).key(eq("customKey"), any(), any());
        assertEquals("generatedKey", generatedKey);
    }

    @Test
    void testGenerateSimpleKeyWhileKeyIsNotSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);

        when(cacheMetadata.getTarget()).thenReturn(getClass());
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{});

        CacheSupport.CacheOperationContext context = cacheSupport.new CacheOperationContext(cacheMetadata, null);
        Object generatedKey = context.generateKey(EMPTY_OBJECT);

        verify(cacheExpressionEvaluator, never()).key(any(), any(), any());
        assertInstanceOf(SimpleKey.class, generatedKey);
    }

    @Test
    void testGenerateCombinedSimpleKeyWhileKeyIsNotSet() {
        CacheMetadata cacheMetadata = mock(CacheMetadata.class);

        when(cacheMetadata.getTarget()).thenReturn(getClass());
        when(cacheMetadata.getMethod()).thenReturn(getClass().getMethods()[0]);
        when(cacheMetadata.getArgs()).thenReturn(new Object[]{42, "parameterValue"});

        CacheSupport.CacheOperationContext context = cacheSupport.new CacheOperationContext(cacheMetadata, null);
        Object generatedKey = context.generateKey(EMPTY_OBJECT);

        verify(cacheExpressionEvaluator, never()).key(any(), any(), any());
        assertInstanceOf(SimpleKey.class, generatedKey);

        SimpleKey expectedKey = new SimpleKey(42, "parameterValue");
        assertEquals(expectedKey, generatedKey);
    }
}
