package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapVerifierTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionForNegativeMaxDepth() {
        MapVerifier.verifyMap(null, null, -1);
    }

    @Test
    public void shouldVerifyNullVsNulCase() {
        MapVerificationResult result = MapVerifier.verifyMap(null, null, 0);
        Assert.assertTrue(result.isVerified());
        result = MapVerifier.verifyMap(null, null, 1);
        Assert.assertTrue(result.isVerified());
        result = MapVerifier.verifyMap(null, null, 2);
        Assert.assertTrue(result.isVerified());
        result = MapVerifier.verifyMap(null, null, 1000);
        Assert.assertTrue(result.isVerified());
    }

    @Test
    public void shouldNotVerifyNullVsNonNullCase() {
        MapVerificationResult result = MapVerifier.verifyMap(new HashMap<String, Object>(), null, 999);
        Assert.assertArrayEquals(new Object[] { "Map is expected to be non-null, but is actually null." },
                result.getAllIssues().toArray());
    }

    @Test
    public void shouldNotVerifyNoneNullVsNullCase() {
        MapVerificationResult result = MapVerifier.verifyMap(null, new HashMap<String, Object>(), 999);
        Assert.assertArrayEquals(new Object[] { "Map is expected to be null, but is actually not." },
                result.getAllIssues().toArray());
    }

    @Test
    public void shouldVerifyEmptyVsEmptyCase() {
        MapVerificationResult result = MapVerifier.verifyMap(new HashMap<String, Object>(),
                new HashMap<String, Object>(), 0);
        Assert.assertEquals(0, result.getAllIssues().size());
        result = MapVerifier.verifyMap(new ConcurrentHashMap<String, Object>(), new LinkedHashMap<String, Object>(),
                0);
        Assert.assertTrue(result.isVerified());
    }

    @Test
    public void shouldVerifySimpleMapObjectWithItself() {
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("key", "value");
        MapVerificationResult result = MapVerifier.verifyMap(expected, expected, 0);
        Assert.assertTrue(result.isVerified());
    }

    @Test
    public void shouldVerifySimpleMapsOfSameContentWithoutWildcards() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key", "value");
        actual.put("key", "value");
        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertEquals(0, result.getAllIssues().size());

        expected.put("key2", "value2");
        actual.put("key2", "value2");
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertEquals(0, result.getAllIssues().size());

        expected.put("key3", 333.333);
        actual.put("key3", 333.333);
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertTrue(result.isVerified());
    }

    @Test
    public void shouldVerifySimpleMapOfAcceptableConentWithWildcards() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> expectedBody = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();
        Map<String, Object> actualBody = new HashMap<String, Object>();

        expected.put("responseCode", 400);
        expected.put("body", expectedBody);
        expectedBody.put("exception", "uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException");
        expectedBody.put("timestamp", "[[DONT_CARE]]");
        expectedBody.put("status", 400);
        expectedBody.put("error", "Bad Request");
        expectedBody.put("message", "Unknown sort direction: someInvalidSortDirection");
        expectedBody.put("path", "[[DONT_CARE]]");
        expectedBody.put("details", null);
        expectedBody.put("callbackErrors", null);
        expectedBody.put("callbackWarnings", null);



        actual.put("responseCode", 400);
        actual.put("body", actualBody);
        actualBody.put("exception", "uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException");
        actualBody.put("timestamp", "2019-11-13T14:02:43.431");
        actualBody.put("status", 400);
        actualBody.put("error", "Bad Request");
        actualBody.put("message", "Unknown sort direction: someInvalidSortDirection");
        actualBody.put("path",
                "/caseworkers/bfb6eeaa-cbcd-466d-aafa-07fe99e7462b/jurisdictions/AUTOTEST1/case-types/AAT/cases/pagination_metadata");
        actualBody.put("details", null);
        actualBody.put("callbackErrors", null);
        actualBody.put("callbackWarnings", null);

        
        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertEquals(0, result.getAllIssues().size());
        Assert.assertTrue(result.isVerified());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithUnexpectedFields() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        actual.put("key1", "value1");
        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse has unexpected field(s): [key1] This may be an undesirable information exposure!" },
                result.getAllIssues().toArray());

        actual.put("number", 15);
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse has unexpected field(s): [key1, number] This may be an undesirable information exposure!" },
                result.getAllIssues().toArray());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithUnavailableFields() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse lacks [key1] field(s) that was/were actually expected to be there." },
                result.getAllIssues().toArray());

        expected.put("number", 15);
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse lacks [key1, number] field(s) that was/were actually expected to be there." },
                result.getAllIssues().toArray());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithUnexpectedAndUnavailableFields() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        actual.put("key1", "value1");
        expected.put("key20", "samevalue");
        actual.put("key21", "samevalue");
        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(
                new Object[] {
                        "actualResponse has unexpected field(s): [key21] This may be an undesirable information exposure!",
                        "actualResponse lacks [key20] field(s) that was/were actually expected to be there." },
                result.getAllIssues().toArray());

        expected.put("key30", "samevalue");
        actual.put("key31", "samevalue");
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(
                new Object[] {
                        "actualResponse has unexpected field(s): [key21, key31] This may be an undesirable information exposure!",
                        "actualResponse lacks [key20, key30] field(s) that was/were actually expected to be there." },
                result.getAllIssues().toArray());
    }

    @Test
    public void shouldNotVerifySimpleMapsWithBadValues() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        actual.put("key1", "value1");
        expected.put("key2", "value2");
        actual.put("key2", "value2_bad");
        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertFalse(result.isVerified());
        Assert.assertArrayEquals(
                new Object[] {
                        "actualResponse contains 1 bad value(s): [key2: expected 'value2' but got 'value2_bad']" },
                result.getAllIssues().toArray());

        expected.put("key3", "value3");
        actual.put("key3", "value3_bad");
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(
                new Object[] {
                        "actualResponse contains 2 bad value(s): [key2: expected 'value2' but got 'value2_bad', key3: expected 'value3' but got 'value3_bad']" },
                result.getAllIssues().toArray());

        expected.put("key30", "samevalue");
        actual.put("key31", "samevalue");
        result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse has unexpected field(s): [key31] This may be an undesirable information exposure!",
                "actualResponse lacks [key30] field(s) that was/were actually expected to be there.",
                "actualResponse contains 2 bad value(s): [key2: expected 'value2' but got 'value2_bad', key3: expected 'value3' but got 'value3_bad']"
                },
                result.getAllIssues().toArray());
    }

    @Test
    public void shouldVerifyCascadedMapsWithSameValuesWithoutWildcards() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        actual.put("key1", "value1");

        Map<String, Object> submap = new ConcurrentHashMap<String, Object>();
        expected.put("key2", submap);
        actual.put("key2", submap);
        submap.put("subfield1", "subfield1_value");
        submap.put("subfield2", "subfield2_value");

        Map<String, Object> subsubmap = new ConcurrentHashMap<String, Object>();
        submap.put("subsubmap", subsubmap);
        subsubmap.put("subsubfield1", "subsubfield1_value");
        subsubmap.put("subsubfield2", "subsubfield2_value");

        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertTrue(result.isVerified());
    }

    // @Ignore
    @Test
    public void shouldNotVerifyCascadedMapsWithSameValuesWithoutWildcards() {
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = new ConcurrentHashMap<String, Object>();

        expected.put("key1", "value1");
        actual.put("key1", "value1");

        Map<String, Object> submap1 = new ConcurrentHashMap<String, Object>();
        Map<String, Object> submap2 = new ConcurrentHashMap<String, Object>();
        expected.put("submap", submap1);
        actual.put("submap", submap2);
        submap1.put("submapkey", "submapvalue");
        submap2.put("submapkey", "submapvalue");

        Map<String, Object> subsubmap1 = new ConcurrentHashMap<String, Object>();
        Map<String, Object> subsubmap2 = new ConcurrentHashMap<String, Object>();
        submap1.put("subsubmap", subsubmap1);
        submap2.put("subsubmap", subsubmap2);
        subsubmap1.put("subsubmapfield1", "subsubmapfield1_value");
        subsubmap2.put("subsubmapfield1", "subsubmapfield1_value_bad");
        subsubmap1.put("subsubmapfield2", "subsubmapfield2_value");
        subsubmap2.put("subsubmapfield2", "subsubmapfield2_value_bad");

        subsubmap1.put("subsubmapfield3a", "subsubmapfield3");
        subsubmap2.put("subsubmapfield3b", "subsubmapfield3");

        MapVerificationResult result = MapVerifier.verifyMap(expected, actual, 0);
        Assert.assertArrayEquals(new Object[] {
                "actualResponse.submap.subsubmap has unexpected field(s): [subsubmapfield3b] This may be an undesirable information exposure!",
                "actualResponse.submap.subsubmap lacks [subsubmapfield3a] field(s) that was/were actually expected to be there.",
                "actualResponse.submap.subsubmap contains 2 bad value(s): [actualResponse.submap.subsubmap.subsubmapfield1, actualResponse.submap.subsubmap.subsubmapfield2]" },
                result.getAllIssues().toArray());
        Assert.assertFalse(result.isVerified());
    }

}
