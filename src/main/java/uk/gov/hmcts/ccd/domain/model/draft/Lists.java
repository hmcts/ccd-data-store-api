package uk.gov.hmcts.ccd.domain.model.draft;

import java.util.List;
import java.util.Optional;

public final class Lists {

    public static <T> Optional<T> last(List<T> list) {
        return Optional.ofNullable(list.isEmpty() ? null : list.get(list.size() - 1));
    }

    public static <T> Optional<T> safeGet(List<T> list, int index) {
        return index < list.size()
            ? Optional.ofNullable(list.get(index))
            : Optional.empty();
    }

    private Lists() {
        // utility class constructor
    }
}
