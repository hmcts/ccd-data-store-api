package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import lombok.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> Set<T> setsIntersection(@NonNull final Set<T> setA, @NonNull final Set<T> setB) {
        Set<T> a = new HashSet<>(setA);
        Set<T> b = new HashSet<>(setB);

        return a.retainAll(b) ? Collections.unmodifiableSet(a) : setA;
    }

    public static <T> List<T> listsUnion(@NonNull final List<T> listA, @NonNull final List<T> listB) {
        return Stream.concat(listA.stream(), listB.stream())
            .collect(Collectors.toUnmodifiableList());
    }
}
