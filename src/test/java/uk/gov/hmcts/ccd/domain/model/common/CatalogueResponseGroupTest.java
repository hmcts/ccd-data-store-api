package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class CatalogueResponseGroupTest {

    @Test
    public void shouldReturnCCDFormattedCode() {

        // ARRANGE
        final String expected = "CCD.02";
        final CatalogueResponseGroup testGroup = CatalogueResponseGroup.CALLBACK;

        // ACT
        final String actual = testGroup.getCode();

        // ASSERT
        assertThat(actual, is(equalTo(expected)));

    }

}
