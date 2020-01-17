package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponse;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponseCode;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class ApiExceptionTest {

    @Test
    public void shouldExtractMessageFromCatalogueResponse() {

        // ARRANGE
        final CatalogueResponse testCatalogueResponse =
            new CatalogueResponse(CatalogueResponseCode.CALLBACK_FAILURE);

        // ACT
        final ApiException cut = new ApiException(testCatalogueResponse);
        final String actualMessage = cut.getMessage();

        // ASSERT
        assertThat(actualMessage, is(equalTo(testCatalogueResponse.getMessage())));

    }

    @Test
    public void shouldAllowOverrideOfMessageFromCatalogueResponse() {

        // ARRANGE
        final String expectedMessage = "my override message";
        final CatalogueResponse testCatalogueResponse =
            new CatalogueResponse(CatalogueResponseCode.CALLBACK_FAILURE);

        // ACT
        final ApiException cut = new ApiException(testCatalogueResponse, expectedMessage);
        final String actualMessage = cut.getMessage();

        // ASSERT
        assertThat(actualMessage, is(not(equalTo(testCatalogueResponse.getMessage()))));
        assertThat(actualMessage, is(equalTo(expectedMessage)));

    }

    @Test
    public void shouldTakeOptionalCallbackErrors() {

        // ARRANGE
        final List<String> expectedErrors = Arrays.asList("Errors: E1", "Errors: E2");

        // ACT
        final ApiException cut = new ApiException("My test with errors")
            .withErrors(expectedErrors);

        // ASSERT
        assertThat(cut.getCallbackErrors(), is(equalTo(expectedErrors)));

    }

    @Test
    public void shouldTakeOptionalCallbackWarnings() {

        // ARRANGE
        final List<String> expectedWarnings = Arrays.asList("Warnings: W1", "Warnings: W2");

        // ACT
        final ApiException cut = new ApiException("My test with warnings")
            .withWarnings(expectedWarnings);

        // ASSERT
        assertThat(cut.getCallbackWarnings(), is(equalTo(expectedWarnings)));

    }

    @Test
    public void shouldTakeOptionalDetails() {

        // ARRANGE
        final String expectedDetails = "Test details";

        // ACT
        final ApiException cut = new ApiException("My test with details")
            .withDetails(expectedDetails);

        // ASSERT
        assertThat(cut.getDetails(), is(equalTo(expectedDetails)));
    }

    @Test
    public void shouldSetCatalogueResponseFromConstructor() {

        // ARRANGE
        final CatalogueResponse expectedCatalogueResponse =
            new CatalogueResponse(CatalogueResponseCode.CALLBACK_FAILURE);

        // ACT
        final ApiException cut = new ApiException(expectedCatalogueResponse);

        // ASSERT
        assertThat(cut.getCatalogueResponse(), is(equalTo(expectedCatalogueResponse)));
    }

    @Test
    public void shouldSetCauseFromConstructor() {

        // ARRANGE
        final Exception expectedCause = new Exception("My test exception");

        // ACT
        final ApiException cut = new ApiException("My Test", expectedCause);

        // ASSERT
        assertThat(cut.getCause(), is(equalTo(expectedCause)));
    }

}
