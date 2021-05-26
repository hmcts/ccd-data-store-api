package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import lombok.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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

    public static <T> Map<T, T> mapsUnion(@NonNull final Map<T, T> mapA, @NonNull final Map<T, T> mapB) {
        return Stream.of(mapA, mapB)
            .flatMap(x -> x.entrySet().stream())
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
