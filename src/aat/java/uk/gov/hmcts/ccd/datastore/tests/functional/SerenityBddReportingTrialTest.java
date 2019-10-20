package uk.gov.hmcts.ccd.datastore.tests.functional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.serenitybdd.junit.runners.SerenityRunner;

@RunWith(SerenityRunner.class)
public class SerenityBddReportingTrialTest {

    @Test
    public void testTheObvious() {
        Assert.assertEquals(1, 1);
    }

}
