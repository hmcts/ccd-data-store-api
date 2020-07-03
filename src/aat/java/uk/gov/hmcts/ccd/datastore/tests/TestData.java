package uk.gov.hmcts.ccd.datastore.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TestData {

    private static final TestData TEST_DATA_LOADER = new TestData();
    private final Map<String, Object> data = new HashMap<>();

    private TestData() {
    }

    public static TestData getInstance() {
        return TEST_DATA_LOADER;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public static String uniqueReference() {
        return UUID.randomUUID().toString();
    }
}
