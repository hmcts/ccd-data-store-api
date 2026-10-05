package uk.gov.hmcts.ccd.datastore.befta;

import java.util.List;
import java.util.Map;

final class GlobalSearchTestLocation {

    private GlobalSearchTestLocation() {
    }

    static Map<?, ?> select(List<?> locations) {
        return locations.stream()
            .filter(Map.class::isInstance)
            .map(value -> (Map<?, ?>) value)
            .filter(location -> List.of("epimms_id", "building_location_name", "region_id", "region").stream()
                .allMatch(field -> location.get(field) instanceof String value && !value.isBlank()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No building location has complete top-level region data"));
    }
}
