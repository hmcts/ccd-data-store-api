package uk.gov.hmcts.ccd.copyoncache;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class CopySupportTest {

    @Test
    void testCreateCopyWithCopyableWithoutMock() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("createCopy");
        TestCopyable copyableFromCache = new TestCopyable();
        Object result = copySupport.createCopy(method, copyableFromCache);

        assertNotNull(result);
        assertInstanceOf(TestCopyable.class, result);
        assertNotSame(copyableFromCache, result);
    }

    @Test
    void testCreateCopyWithListCopyableWithoutMock() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("createCopy");
        TestCopyable copyable1 = new TestCopyable();
        TestCopyable copyable2 = new TestCopyable();
        List<TestCopyable> copyableListFromCache = Arrays.asList(copyable1, copyable2);

        Object result = copySupport.createCopy(method, copyableListFromCache);

        assertNotNull(result);
        assertInstanceOf(List.class, result);

        List<?> resultList = (List<?>) result;
        assertEquals(copyableListFromCache.size(), resultList.size());

        for (int i = 0; i < copyableListFromCache.size(); i++) {
            assertInstanceOf(TestCopyable.class, resultList.get(i));
            assertNotSame(copyableListFromCache.get(i), resultList.get(i));
        }
    }

    @Test
    void testCreateCopyWithCopyable() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("createCopy");
        TestCopyable copyableFromCache = mock(TestCopyable.class);
        when(copyableFromCache.createCopy()).thenReturn(new TestCopyable());
        Object result = copySupport.createCopy(method, copyableFromCache);

        assertInstanceOf(TestCopyable.class, result);
        verify(copyableFromCache, times(1)).createCopy();
    }

    @Test
    void testCreateCopyWithListCopyable() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("createCopy");
        TestCopyable copyableFromCache = mock(TestCopyable.class);
        when(copyableFromCache.createCopy()).thenReturn(new TestCopyable());
        List<Copyable<?>> copyableList = Arrays.asList(copyableFromCache, copyableFromCache);

        Object result = copySupport.createCopy(method, copyableList);

        assertInstanceOf(List.class, result);
        verify(copyableFromCache, times(2)).createCopy();
    }

    @Test
    void testCreateCopyWithOptionalCopyable() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("getOptionalCopyable");
        TestCopyable copyable = mock(TestCopyable.class);
        when(copyable.createCopy()).thenReturn(new TestCopyable());

        Object result = copySupport.createCopy(method, Optional.of(copyable));

        assertInstanceOf(Optional.class, result);
        assertInstanceOf(Copyable.class, ((Optional<?>) result).orElse(null));
    }

    @Test
    void testCreateCopyWithOptionalListCopyable() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("getOptionalListCopyable");
        TestCopyable copyableFromCache = mock(TestCopyable.class);
        when(copyableFromCache.createCopy()).thenReturn(new TestCopyable());
        Optional<List<TestCopyable>> copyableList = Optional.of(Arrays.asList(copyableFromCache, copyableFromCache));

        Object result = copySupport.createCopy(method, copyableList);

        verify(copyableFromCache, times(2)).createCopy();
        assertInstanceOf(Optional.class, result);
        assertInstanceOf(List.class, ((Optional<?>) result).orElse(null));
        assertInstanceOf(Copyable.class, ((List<?>) ((Optional<?>) result).orElse(null)).get(0));
        assertInstanceOf(Copyable.class, ((List<?>) ((Optional<?>) result).orElse(null)).get(1));
    }

    @Test
    void testCreateCopyWithOptionalListCopyableWithNull() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("getOptionalListCopyable");
        TestCopyable copyableFromCache = mock(TestCopyable.class);
        when(copyableFromCache.createCopy()).thenReturn(new TestCopyable());
        Optional<List<TestCopyable>> copyableList = Optional.of(Arrays.asList(copyableFromCache,
            null, copyableFromCache));

        Object result = copySupport.createCopy(method, copyableList);

        verify(copyableFromCache, times(2)).createCopy();
        assertInstanceOf(Optional.class, result);
        assertInstanceOf(List.class, ((Optional<?>) result).orElse(null));
        assertInstanceOf(Copyable.class, ((List<?>) ((Optional<?>) result).orElse(null)).get(0));
        assertNull(((List<?>) ((Optional<?>) result).orElse(null)).get(1));
        assertInstanceOf(Copyable.class, ((List<?>) ((Optional<?>) result).orElse(null)).get(2));
    }

    @Test
    void testCreateCopyWithUnsupportedType() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("getUnsupportedOptionalType");

        assertThrows(UnsupportedOperationException.class,
            () -> copySupport.createCopy(method, "unsupported"));
    }

    @Test
    void testCreateCopyWithOptionalUnsupportedType() throws NoSuchMethodException {
        CopySupport copySupport = new CopySupport();
        Method method = TestCopyable.class.getMethod("getUnsupportedOptionalType");

        assertThrows(UnsupportedOperationException.class,
            () -> copySupport.createCopy(method, Optional.of("unsupported")));
    }

    @SuppressWarnings("unused")
    private static class TestCopyable implements Copyable<TestCopyable> {

        @Override
        public TestCopyable createCopy() {
            return new TestCopyable();
        }

        public Optional<Copyable<?>> getOptionalCopyable() {
            return Optional.empty();
        }

        public Optional<List<Copyable<?>>> getOptionalListCopyable() {
            return Optional.empty();
        }

        public String getUnsupportedType() {
            return "";
        }

        public Optional<String> getUnsupportedOptionalType() {
            return Optional.empty();
        }
    }
}
