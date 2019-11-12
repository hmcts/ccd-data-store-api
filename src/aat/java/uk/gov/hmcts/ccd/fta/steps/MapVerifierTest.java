package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

}
