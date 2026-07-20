package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CopyableTest {

    private final TestCopyable copyable = new TestCopyable("root");

    @Test
    void shouldReturnNullWhenCopyingNullList() {
        assertNull(copyable.createCopyList(null));
    }

    @Test
    void shouldCreateCopyList() {
        TestCopyable original = new TestCopyable("value");

        List<TestCopyable> copiedList = copyable.createCopyList(List.of(original));

        assertAll(
            () -> assertEquals(1, copiedList.size()),
            () -> assertNotSame(original, copiedList.getFirst()),
            () -> assertEquals(original.value, copiedList.getFirst().value)
        );
    }

    @Test
    void shouldReturnNullWhenCopyingNullAclList() {
        assertNull(copyable.createACLCopyList(null));
    }

    @Test
    void shouldReturnSameEmptyAclList() {
        List<AccessControlList> emptyList = List.of();

        assertSame(emptyList, copyable.createACLCopyList(emptyList));
    }

    @Test
    void shouldCreateAclCopyListWithRegularAndComplexAcls() {
        AccessControlList acl = acl("caseworker", true, true, false, false);
        ComplexACL complexAcl = complexAcl("Field.Nested", "complex-role", false, true, true, true);

        List<AccessControlList> copiedList = copyable.createACLCopyList(List.of(acl, complexAcl));

        AccessControlList copiedAcl = copiedList.get(0);
        ComplexACL copiedComplexAcl = assertInstanceOf(ComplexACL.class, copiedList.get(1));

        assertAll(
            () -> assertNotSame(acl, copiedAcl),
            () -> assertEquals(acl.getAccessProfile(), copiedAcl.getAccessProfile()),
            () -> assertEquals(acl.isCreate(), copiedAcl.isCreate()),
            () -> assertNotSame(complexAcl, copiedComplexAcl),
            () -> assertEquals(complexAcl.getListElementCode(), copiedComplexAcl.getListElementCode()),
            () -> assertEquals(complexAcl.getAccessProfile(), copiedComplexAcl.getAccessProfile()),
            () -> assertEquals(complexAcl.isRead(), copiedComplexAcl.isRead()),
            () -> assertEquals(complexAcl.isUpdate(), copiedComplexAcl.isUpdate()),
            () -> assertEquals(complexAcl.isDelete(), copiedComplexAcl.isDelete())
        );
    }

    private AccessControlList acl(String role, boolean create, boolean read, boolean update, boolean delete) {
        AccessControlList acl = new AccessControlList();
        acl.setAccessProfile(role);
        acl.setCreate(create);
        acl.setRead(read);
        acl.setUpdate(update);
        acl.setDelete(delete);
        return acl;
    }

    private ComplexACL complexAcl(String listElementCode,
                                  String role,
                                  boolean create,
                                  boolean read,
                                  boolean update,
                                  boolean delete) {
        ComplexACL acl = new ComplexACL();
        acl.setListElementCode(listElementCode);
        acl.setAccessProfile(role);
        acl.setCreate(create);
        acl.setRead(read);
        acl.setUpdate(update);
        acl.setDelete(delete);
        return acl;
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
