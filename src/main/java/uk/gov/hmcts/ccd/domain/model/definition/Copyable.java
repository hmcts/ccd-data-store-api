package uk.gov.hmcts.ccd.domain.model.definition;

import java.util.ArrayList;
import java.util.List;

public interface Copyable<T>  {

    T createCopy();

    default <S extends Copyable<S>> List<S> createDeepCopyList(List<S> originalList) {
        if (originalList == null) {
            return null;
        }

        List<S> copyList = new ArrayList<>(originalList.size());
        for (S item : originalList) {
            copyList.add(item.createCopy());
        }
        return copyList;
    }

    default <S> List<S> createShallowCopyList(List<S> originalList) {
        if (originalList == null) {
            return null;
        }
        return new ArrayList<>(originalList);
    }
}
