package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapVerifierTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionFoprNegativeMaxDepth() {
        MapVerifier.verifyMap(null, null, -1);
    }

    @Test
    public void shouldVerifyNullVsNulCase() {
        List<String> messages = MapVerifier.verifyMap(null, null, 0);
        Assert.assertEquals(0, messages.size());
        messages = MapVerifier.verifyMap(null, null, 1);
        Assert.assertEquals(0, messages.size());
        messages = MapVerifier.verifyMap(null, null, 2);
        Assert.assertEquals(0, messages.size());
        messages = MapVerifier.verifyMap(null, null, 1000);
        Assert.assertEquals(0, messages.size());
    }

    @Test
    public void shouldNotVerifyNullVsNonNullCase() {
        List<String> messages = MapVerifier.verifyMap(new HashMap<String, Object>(), null, 999);
        Assert.assertArrayEquals(new Object[] { "Map is expected to be non-null, but is actuall null." },
                messages.toArray());
    }

    @Test
    public void shouldNotVerifyNoneNullVsNullCase() {
        List<String> messages = MapVerifier.verifyMap(null, new HashMap<String, Object>(), 999);
        Assert.assertArrayEquals(new Object[] { "Map is expected to be null, but is actuall not." },
                messages.toArray());
    }

    @Test
    public void shouldVerifyEmptyVsEmptyCase() {
        List<String> messages = MapVerifier.verifyMap(new HashMap<String, Object>(), new HashMap<String, Object>(), 0);
        Assert.assertEquals(0, messages.size());
        messages = MapVerifier.verifyMap(new ConcurrentHashMap<String, Object>(), new LinkedHashMap<String, Object>(),
                0);
        Assert.assertEquals(0, messages.size());
    }

    @Test
    public void shouldVerifySimpleMapObjectWithItself() {
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("key", "value");
        List<String> messages = MapVerifier.verifyMap(expected, expected, 0);
        Assert.assertEquals(0, messages.size());
    }

    @Test
    public void shouldVerifySimpleMapsOfSameConentWithoutWildcards() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key", "value");
        actual.put("key", "value");
        List<String> messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertEquals(0, messages.size());

        expected.put("key2", "value2");
        actual.put("key2", "value2");
        messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertEquals(0, messages.size());

        expected.put("key3", 333.333);
        actual.put("key3", 333.333);
        messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertEquals(0, messages.size());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithUnexpectedFields() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        actual.put("key1", "value1");
        List<String> messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse has unexpected field(s): [key1] This may be an undesirable information exposure!" },
                messages.toArray());

        actual.put("number", 15);
        messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse has unexpected field(s): [key1, number] This may be an undesirable information exposure!" },
                messages.toArray());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithUnavailableFields() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        List<String> messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse lacks [key1] field(s) that was/were actually expected to be there." },
                messages.toArray());

        expected.put("number", 15);
        messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse lacks [key1, number] field(s) that was/were actually expected to be there." },
                messages.toArray());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithUnexpectedAndUnavailableFields() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        actual.put("key1", "value1");
        expected.put("key20", "samevalue");
        actual.put("key21", "samevalue");
        List<String> messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(
                new Object[] {
                        "actualResponse has unexpected field(s): [key21] This may be an undesirable information exposure!",
                        "actualResponse lacks [key20] field(s) that was/were actually expected to be there." },
                messages.toArray());

        expected.put("key30", "samevalue");
        actual.put("key31", "samevalue");
        messages = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(
                new Object[] {
                        "actualResponse has unexpected field(s): [key21, key31] This may be an undesirable information exposure!",
                        "actualResponse lacks [key20, key30] field(s) that was/were actually expected to be there." },
                messages.toArray());
    }

}
