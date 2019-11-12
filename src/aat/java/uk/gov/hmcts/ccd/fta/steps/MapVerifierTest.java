package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class MapVerifierTest {

    @Test
    public void shouldVerifyNullMaps() {
        List<String> messages = MapVerifier.verifyMap(null, null, 0);
        Assert.assertEquals(0, messages.size());
        messages = MapVerifier.verifyMap(null, null, 1);
        Assert.assertEquals(0, messages.size());
        messages = MapVerifier.verifyMap(null, null, 2);
        Assert.assertEquals(0, messages.size());
        MapVerifier.verifyMap(null, null, 1000);
        Assert.assertEquals(0, messages.size());
    }
}
