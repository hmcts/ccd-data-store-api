package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopyableTest {

    private final TestCopyable copyable = new TestCopyable("root");

    @Test
    void shouldReturnNullWhenDeepCopyingNullList() {
        assertNull(copyable.createDeepCopyList(null));
    }

    @Test
    void shouldCreateDeepCopyList() {
        TestCopyable original = new TestCopyable("value");

        List<TestCopyable> copiedList = copyable.createDeepCopyList(List.of(original));

        assertAll(
            () -> assertEquals(1, copiedList.size()),
            () -> assertNotSame(original, copiedList.get(0)),
            () -> assertEquals(original.value, copiedList.get(0).value)
        );
    }

    @Test
    void shouldReturnEmptyListWhenShallowCopyingNullList() {
        List<TestCopyable> copiedList = copyable.createShallowCopyList(null);

        assertTrue(copiedList.isEmpty());
    }

    @Test
    void shouldCreateShallowCopyList() {
        TestCopyable original = new TestCopyable("value");
        List<TestCopyable> originalList = List.of(original);

        List<TestCopyable> copiedList = copyable.createShallowCopyList(originalList);

        assertAll(
            () -> assertEquals(1, copiedList.size()),
            () -> assertNotSame(originalList, copiedList),
            () -> assertSame(original, copiedList.get(0))
        );
    }

    private static class TestCopyable implements Copyable<TestCopyable> {

        private final String value;

        private TestCopyable(String value) {
            this.value = value;
        }

        @Override
        public TestCopyable createCopy() {
            return new TestCopyable(value);
        }
    }
}
