package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class CatalogueResponseCodeTest {

    @Test
    public void shouldReturnCCDFormattedCode() {

        // ARRANGE
        final String expected = "CCD.02.01";
        final CatalogueResponseCode testCode = CatalogueResponseCode.CALLBACK_FAILURE;

        // ACT
        final String actual = testCode.getCode();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

    @Test
    public void shouldReturnGroupInformation() {

        final CatalogueResponseGroup expected = CatalogueResponseGroup.CALLBACK;
        final CatalogueResponseCode testCode = CatalogueResponseCode.CALLBACK_FAILURE;

        // ACT
        final CatalogueResponseGroup actual = testCode.getGroup();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

    @Test
    public void shouldReturnMessageInformation() {

        // ARRANGE
        final String expected = "Callback failure.";
        final CatalogueResponseCode testCode = CatalogueResponseCode.CALLBACK_FAILURE;

        // ACT
        final String actual = testCode.getMessage();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

}
