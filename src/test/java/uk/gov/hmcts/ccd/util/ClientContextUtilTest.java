package uk.gov.hmcts.ccd.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ClientContextUtilTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Logger mockLogger;

    @BeforeEach
    public void setUp() {
        mockLogger = mock(Logger.class);
        ClientContextUtil.LOG = mockLogger;
    }

    @Test
    void testMergeClientContextsBothValid() throws IOException {
        String jsonA = "{\"key1\": \"value1\", \"key2\": \"value2\"}";
        String jsonB = "{\"key2\": \"newValue2\", \"key3\": \"value3\"}";
        String expectedMergedJson = "{\"key1\":\"value1\",\"key2\":\"newValue2\",\"key3\":\"value3\"}";

        String result = ClientContextUtil.mergeClientContexts(jsonA, jsonB);

        assertEquals(expectedMergedJson, result);
    }

    @Test
    void testMergeClientContextsInvalidJsonA() {
        String invalidJsonA = "invalid json";
        String jsonB = "{\"key2\":\"newValue2\",\"key3\":\"value3\"}";
        String expectedMergedJson = "{\"key2\":\"newValue2\",\"key3\":\"value3\"}";

        String result = ClientContextUtil.mergeClientContexts(invalidJsonA, jsonB);

        assertEquals(expectedMergedJson, result);
        verify(mockLogger).error(Mockito.anyString(), Mockito.eq(invalidJsonA), Mockito.any(IOException.class));
    }

    @Test
    void testMergeClientContextsInvalidJsonB() {
        String jsonA = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        String invalidJsonB = "invalid json";

        String result = ClientContextUtil.mergeClientContexts(jsonA, invalidJsonB);

        assertEquals(jsonA, result);
        verify(mockLogger).error(Mockito.anyString(), Mockito.eq(invalidJsonB), Mockito.any(IOException.class));
    }

    @Test
    void testMergeClientContextsBothInvalid() {
        String invalidJsonA = "invalid json A";
        String invalidJsonB = "invalid json B";

        String result = ClientContextUtil.mergeClientContexts(invalidJsonA, invalidJsonB);

        assertEquals(invalidJsonA, result);
        verify(mockLogger).error(Mockito.anyString(), Mockito.eq(invalidJsonA), Mockito.any(IOException.class));
        verify(mockLogger).error(Mockito.anyString(), Mockito.eq(invalidJsonB), Mockito.any(IOException.class));
    }
}
