package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class CatalogueResponseElementTest {

    @Test
    public void shouldReturnCCDFormattedCode() {

        // ARRANGE
        final String expected = "CCD.02.01";
        final CatalogueResponseElement testElement = CatalogueResponseElement.CALLBACK_FAILURE;

        // ACT
        final String actual = testElement.getCode();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

    @Test
    public void shouldReturnGroupInformation() {

        final CatalogueResponseGroup expected = CatalogueResponseGroup.CALLBACK;
        final CatalogueResponseElement testElement = CatalogueResponseElement.CALLBACK_FAILURE;

        // ACT
        final CatalogueResponseGroup actual = testElement.getGroup();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

    @Test
    public void shouldReturnMessageInformation() {

        // ARRANGE
        final String expected = "Callback failure.";
        final CatalogueResponseElement testElement = CatalogueResponseElement.CALLBACK_FAILURE;

        // ACT
        final String actual = testElement.getMessage();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

}
