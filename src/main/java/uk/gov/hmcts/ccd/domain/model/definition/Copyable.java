package uk.gov.hmcts.ccd.domain.model.definition;

import java.util.ArrayList;
import java.util.List;

public interface Copyable<T>  {

    T createCopy();

    default <S extends Copyable<S>> List<S> createCopyList(List<S> originalList) {
        if (originalList == null) {
            return null;
        }

        List<S> copyList = new ArrayList<>(originalList.size());
        for (S item : originalList) {
            copyList.add(item.createCopy());
        }
        return copyList;
    }

    default List<AccessControlList> createACLCopyList(List<AccessControlList> accessControlLists) {
        if (accessControlLists == null || accessControlLists.isEmpty()) {
            return accessControlLists;
        }

        List<AccessControlList> copiedACLs = new ArrayList<>(accessControlLists.size());
        for (AccessControlList accessControlList : accessControlLists) {
            if (accessControlList instanceof ComplexACL) {
                copiedACLs.add(((ComplexACL) accessControlList).deepCopy());
            } else {
                copiedACLs.add(accessControlList.createCopy());
            }
        }

        return copiedACLs;
    }
}
