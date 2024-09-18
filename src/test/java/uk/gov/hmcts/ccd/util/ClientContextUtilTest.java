package uk.gov.hmcts.ccd.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientContextUtilTest {

    @Test
    void testMergeClientContextsBothValidAndEnclosedInBrackets() {
        String originalJson = "{\"key1\": \"value1\", \"key2\": \"value2\"}";
        String toBeMergedJson = "{\"key2\": \"newValue2\", \"key3\": \"value3\"}";
        String expectedMergedJson = "{\"key1\":\"value1\",\"key2\":\"newValue2\",\"key3\":\"value3\"}";

        String originalJsonBracketEncoded = "[" + ClientContextUtil.encodeToBase64(originalJson) + "]";
        String toBeMergedJsonBracketEncoded = "[" + ClientContextUtil.encodeToBase64(toBeMergedJson) + "]";

        String result = ClientContextUtil.mergeClientContexts(originalJsonBracketEncoded, toBeMergedJsonBracketEncoded);

        assertEquals(ClientContextUtil.encodeToBase64(expectedMergedJson), result);
    }

    @Test
    void testMergeClientContextsBothValid() {
        String originalJson = "{\"key1\": \"value1\", \"key2\": \"value2\"}";
        String toBeMergedJson = "{\"key2\": \"newValue2\", \"key3\": \"value3\"}";
        String expectedMergedJson = "{\"key1\":\"value1\",\"key2\":\"newValue2\",\"key3\":\"value3\"}";

        String result = ClientContextUtil.mergeClientContexts(ClientContextUtil.encodeToBase64(originalJson),
            ClientContextUtil.encodeToBase64(toBeMergedJson));

        assertEquals(ClientContextUtil.encodeToBase64(expectedMergedJson), result);
    }

    @Test
    void testMergeClientContextsWithInvalidJsonA() {
        String invalidOriginalJson = "{\"test\":invalid JSON}";
        String toBeMergedJson = "{\"key2\":\"newValue2\",\"key3\":\"value3\"}";
        String expectedMergedJson = "{\"key2\":\"newValue2\",\"key3\":\"value3\"}";

        String result = ClientContextUtil.mergeClientContexts(ClientContextUtil.encodeToBase64(invalidOriginalJson),
            ClientContextUtil.encodeToBase64(toBeMergedJson));

        assertEquals(ClientContextUtil.encodeToBase64(expectedMergedJson), result);
    }

    @Test
    void testMergeClientContextsWithInvalidToBeMergedJson() {
        String originalJson = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        String invalidToBeMergedJson = "invalid To Be Merged JSON";

        String result = ClientContextUtil.mergeClientContexts(ClientContextUtil.encodeToBase64(originalJson),
            ClientContextUtil.encodeToBase64(invalidToBeMergedJson));

        assertEquals(ClientContextUtil.encodeToBase64(originalJson), result);
    }

    @Test
    void testMergeClientContextsWithBothInvalid() {
        String invalidOriginalJson = "invalid original JSON";
        String invalidToBeMergedJson = "invalid to-Be-Merged JSON";

        String result = ClientContextUtil.mergeClientContexts(ClientContextUtil.encodeToBase64(invalidOriginalJson),
            ClientContextUtil.encodeToBase64(invalidToBeMergedJson));

        assertEquals(ClientContextUtil.encodeToBase64(invalidOriginalJson), result);
    }
}
