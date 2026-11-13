package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.befta.data.HttpTestData;
import uk.gov.hmcts.befta.data.ResponseData;
import uk.gov.hmcts.befta.data.RequestData;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.DynamicValueInjector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataStoreTestAutomationAdapterTest {

    private final DataStoreTestAutomationAdapter adapter = new DataStoreTestAutomationAdapter();

    @Test
    void reusesScenarioUuidAcrossCallsAndAdapterInstances() {
        BackEndFunctionalTestScenarioContext context = contextWithTag("S-123.2");
        String value = (String) adapter.calculateCustomValue(context, "UniqueString");

        assertEquals("S-123.2" + UUID.fromString(value.substring("S-123.2".length())), value);
        assertEquals(value, adapter.calculateCustomValue(context, "UniqueString"));
        assertEquals(value, new DataStoreTestAutomationAdapter().calculateCustomValue(context, "UniqueString"));
    }

    @Test
    void sharesScenarioUuidWithChildrenAndNestedPrerequisites() {
        BackEndFunctionalTestScenarioContext root = contextWithTag("S-123.2");
        BackEndFunctionalTestScenarioContext child = contextWithTag("S-001");
        BackEndFunctionalTestScenarioContext sibling = contextWithTag("S-002");
        BackEndFunctionalTestScenarioContext grandchild = contextWithTag("S-003");
        when(child.getParentContext()).thenReturn(root);
        when(sibling.getParentContext()).thenReturn(root);
        when(grandchild.getParentContext()).thenReturn(child);

        Object value = adapter.calculateCustomValue(grandchild, "UniqueString");

        assertEquals(value, adapter.calculateCustomValue(root, "UniqueString"));
        assertEquals(value, adapter.calculateCustomValue(child, "UniqueString"));
        assertEquals(value, adapter.calculateCustomValue(sibling, "UniqueString"));
    }

    @Test
    void isolatesScenariosSharingATagDuringConcurrentAccess() throws Exception {
        BackEndFunctionalTestScenarioContext first = contextWithTag("S-123.2");
        BackEndFunctionalTestScenarioContext second = contextWithTag("S-123.2");
        int workers = 8;
        CyclicBarrier start = new CyclicBarrier(workers);
        var executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                BackEndFunctionalTestScenarioContext context = i % 2 == 0 ? first : second;
                results.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return adapter.calculateCustomValue(context, "UniqueString");
                }));
            }
            Object firstValue = results.get(0).get(10, TimeUnit.SECONDS);
            Object secondValue = results.get(1).get(10, TimeUnit.SECONDS);
            assertNotEquals(firstValue, secondValue);
            for (int i = 0; i < workers; i++) {
                assertEquals(i % 2 == 0 ? firstValue : secondValue, results.get(i).get(10, TimeUnit.SECONDS));
            }
            assertEquals(firstValue, adapter.calculateCustomValue(first, "UniqueString"));
            assertEquals(secondValue, adapter.calculateCustomValue(second, "UniqueString"));
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "NULL"})
    void skipsBuildingsWithIncompleteLocationOrRegion(String missingValue) {
        for (String field : List.of("epimms_id", "building_location_name", "region_id", "region")) {
            Map<String, Object> incomplete = new HashMap<>(validBuilding());
            incomplete.put(field, missingValue);
            Map<String, Object> valid = validBuilding();
            BackEndFunctionalTestScenarioContext context = contextWithLocations(List.of(incomplete, valid));

            assertEquals(valid, adapter.calculateCustomValue(context, "validBuildingLocation"));
        }
    }

    @Test
    void selectsSameCompleteRecordForEveryField() {
        Map<String, Object> first = validBuilding();
        Map<String, Object> second = Map.of("epimms_id", "456", "building_location_name", "Other building",
                                          "region_id", "2", "region", "Other region");
        BackEndFunctionalTestScenarioContext context = contextWithLocations(List.of(first, second));

        assertEquals(first, adapter.calculateCustomValue(context, "validBuildingLocation"));
        assertEquals(first, adapter.calculateCustomValue(context, "validBuildingLocation"));
    }

    @Test
    void resolvesSameBuildingThroughSiblingAndChildContextFormulas() {
        Map<String, Object> incomplete = new HashMap<>(validBuilding());
        incomplete.put("region_id", null);
        BackEndFunctionalTestScenarioContext locationContext = contextWithLocations(
            List.of(incomplete, validBuilding()));
        when(locationContext.getCustomValues()).thenReturn(
            key -> adapter.calculateCustomValue(locationContext, key));
        BackEndFunctionalTestScenarioContext context = mock(BackEndFunctionalTestScenarioContext.class);
        Map<String, BackEndFunctionalTestScenarioContext> contexts = Map.of(
            "Get_RefData_BuildingLocations_Load_All", locationContext);
        when(context.getSiblingContexts()).thenReturn(contexts);
        when(context.getChildContexts()).thenReturn(contexts);

        String sibling = "${[scenarioContext][siblingContexts][Get_RefData_BuildingLocations_Load_All]"
            + "[customValues][validBuildingLocation]";
        String child = "${[scenarioContext][childContexts][Get_RefData_BuildingLocations_Load_All]"
            + "[customValues][validBuildingLocation]";
        Map<String, Object> requestBody = new HashMap<>(Map.of(
            "baseLocation", sibling + "[epimms_id]}", "region", sibling + "[region_id]}"));
        Map<String, Object> expectedBody = new HashMap<>(Map.of(
            "baseLocationId", child + "[epimms_id]}", "regionId", child + "[region_id]}",
            "baseLocationName", child + "[building_location_name]}", "regionName", child + "[region]}"));
        HttpTestData testData = mock(HttpTestData.class);
        RequestData request = mock(RequestData.class);
        ResponseData expected = mock(ResponseData.class);
        when(testData.getRequest()).thenReturn(request);
        when(request.getBody()).thenReturn(requestBody);
        when(testData.getExpectedResponse()).thenReturn(expected);
        when(expected.getBody()).thenReturn(expectedBody);

        DynamicValueInjector injector = new DynamicValueInjector(adapter, testData, context);
        injector.injectDataFromContextBeforeApiCall();
        injector.injectDataFromContextAfterApiCall();

        assertEquals(Map.of("baseLocation", "123", "region", "1"), requestBody);
        assertEquals(Map.of("baseLocationId", "123", "regionId", "1",
                            "baseLocationName", "Test building", "regionName", "Test region"), expectedBody);
    }

    @Test
    void reportsMissingUsableReferenceData() {
        for (Object locations : List.of(List.of(), List.of(Map.of("epimms_id", "123")), "invalid response")) {
            FunctionalTestException exception = assertThrows(FunctionalTestException.class,
                () -> adapter.calculateCustomValue(contextWithLocations(locations), "validBuildingLocation"));
            assertEquals("No building location with non-blank epimms_id, building_location_name, "
                             + "region_id and region found in location reference data", exception.getMessage());
        }
    }

    private Map<String, Object> validBuilding() {
        return Map.of("epimms_id", "123", "building_location_name", "Test building",
                      "region_id", "1", "region", "Test region");
    }

    private BackEndFunctionalTestScenarioContext contextWithTag(String tag) {
        BackEndFunctionalTestScenarioContext context = mock(BackEndFunctionalTestScenarioContext.class);
        when(context.getCurrentScenarioTag()).thenReturn(tag);
        return context;
    }

    private BackEndFunctionalTestScenarioContext contextWithLocations(Object locations) {
        BackEndFunctionalTestScenarioContext context = mock(BackEndFunctionalTestScenarioContext.class);
        HttpTestData testData = mock(HttpTestData.class);
        ResponseData response = mock(ResponseData.class);
        when(context.getTestData()).thenReturn(testData);
        when(testData.getActualResponse()).thenReturn(response);
        when(response.getBody()).thenReturn(Map.of("arrayInMap", locations));
        return context;
    }
}
