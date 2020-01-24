package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CatalogueResponseTest {

    @Test
    public void shouldUseNullsInDefaultConstructor() {

        // ARRANGE

        // ACT
        final CatalogueResponse testCatalogueResponse = new CatalogueResponse();

        // ASSERT
        assertNull(testCatalogueResponse.getCode());
        assertNull(testCatalogueResponse.getMessage());
        assertNull(testCatalogueResponse.getDetails());

    }

    @Test
    public void shouldDeserializeJsonOkViaBuilderWithDetails() throws IOException {

        // ARRANGE
        final Map<String, Object> expectedDetails = new HashMap<>();
        expectedDetails.put("test1", 1);
        expectedDetails.put("test2", "Test String");
        final CatalogueResponse expectedCatalogueResponse =
            new CatalogueResponse(CatalogueResponseElement.CALLBACK_FAILURE, expectedDetails);
        final String testJson = String.format(
            "{ \"code\": \"%s\", \"message\": \"%s\", \"details\": { \"test1\": 1, \"test2\": \"Test String\" } }",
            CatalogueResponseElement.CALLBACK_FAILURE.getCode(), CatalogueResponseElement.CALLBACK_FAILURE.getMessage());
        final ObjectMapper mapper = new ObjectMapper();

        // ACT
        final CatalogueResponse actualCatalogueResponse
            = mapper.readerFor(CatalogueResponse.class).readValue(testJson);

        // ASSERT
        assertTrue(EqualsBuilder.reflectionEquals(expectedCatalogueResponse, actualCatalogueResponse));

    }

    @Test
    public void shouldDeserializeJsonOkViaBuilderWithoutDetails() throws IOException {

        // ARRANGE
        final CatalogueResponse expectedCatalogueResponse =
            new CatalogueResponse(CatalogueResponseElement.CALLBACK_FAILURE);
        final String testJson = String.format(
            "{ \"code\": \"%s\", \"message\": \"%s\" }",
            CatalogueResponseElement.CALLBACK_FAILURE.getCode(), CatalogueResponseElement.CALLBACK_FAILURE.getMessage());
        final ObjectMapper mapper = new ObjectMapper();

        // ACT
        final CatalogueResponse actualCatalogueResponse
            = mapper.readerFor(CatalogueResponse.class).readValue(testJson);

        // ASSERT
        assertThat(actualCatalogueResponse.getCode(), is(equalTo(expectedCatalogueResponse.getCode())));
        assertThat(actualCatalogueResponse.getMessage(), is(equalTo(expectedCatalogueResponse.getMessage())));
        assertNull(actualCatalogueResponse.getDetails());

    }

}
