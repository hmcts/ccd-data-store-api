package uk.gov.hmcts.jsonstore;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;
import uk.gov.hmcts.ccd.fta.steps.MapVerificationResult;
import uk.gov.hmcts.ccd.fta.steps.MapVerifier;

public class JsonStoreWithInheritanceTest {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "framework-test-data/features" };
    private static final HttpTestDataSource TEST_DATA_RESOURCE = new JsonStoreHttpTestDataSource(
            TEST_DATA_RESOURCE_PACKAGES);

    @Test
    public void shouldHaveTheSameDataInBaseRequestAndExtensionResponseBodies() {
        HttpTestData base = TEST_DATA_RESOURCE.getDataForTestCall("F-050_Test_Data_Base");
        HttpTestData extension = TEST_DATA_RESOURCE.getDataForTestCall("S-301");

        @SuppressWarnings("unchecked")
        MapVerificationResult result = new MapVerifier("", 5).verifyMap(
                (Map<String, Object>) base.getRequest().getBody().get("data"),
                (Map<String, Object>) extension.getExpectedResponse().getBody().get("data"));

        Assert.assertTrue(result.isVerified());
    }

    @Test
    public void shouldHaveTheSameDataInExtensionRequestAndExtensionResponseBodies() {
        HttpTestData extension = TEST_DATA_RESOURCE.getDataForTestCall("S-301");

        @SuppressWarnings("unchecked")
        MapVerificationResult result = new MapVerifier("", 5).verifyMap(
                (Map<String, Object>) extension.getRequest().getBody().get("data"),
                (Map<String, Object>) extension.getExpectedResponse().getBody().get("data"));

        Assert.assertTrue(result.isVerified());
    }
}
