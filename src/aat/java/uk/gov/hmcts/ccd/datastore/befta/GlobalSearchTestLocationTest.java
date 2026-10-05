package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalSearchTestLocationTest {

    @Test
    void shouldSkipLocationWithOnlyNestedRegionData() {
        Map<String, Object> birmingham = new HashMap<>();
        birmingham.put("epimms_id", "815833");
        birmingham.put("building_location_name", "Birmingham CTSC");
        birmingham.put("region_id", null);
        birmingham.put("region", null);
        birmingham.put("court_venues", List.of(Map.of("region_id", "2", "region", "Midlands")));
        Map<String, String> aldershot = Map.of(
            "epimms_id", "450049", "building_location_name", "Aldershot Justice Centre",
            "region_id", "6", "region", "South West");

        assertThat(GlobalSearchTestLocation.select(List.of(birmingham, aldershot))).isSameAs(aldershot);
    }

    @Test
    void shouldFailClearlyWhenNoLocationHasCompleteRegionData() {
        Map<String, String> incomplete = Map.of(
            "epimms_id", "815833", "building_location_name", "Birmingham CTSC",
            "region_id", " ", "region", "Midlands");

        assertThatThrownBy(() -> GlobalSearchTestLocation.select(List.of(incomplete)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No building location has complete top-level region data");
    }
}
