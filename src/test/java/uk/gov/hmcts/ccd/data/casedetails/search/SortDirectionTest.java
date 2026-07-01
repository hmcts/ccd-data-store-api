package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class SortDirectionTest {

    @Test
    public void testFromNullString() {
        assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.empty()));
    }

    @Test
    public void testFromAscCaseInsensitive() {
        assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.of("AsC")));
    }

    @Test
    public void testFromDescCaseInsensitive() {
        assertEquals(SortDirection.DESC, SortDirection.fromOptionalString(Optional.of("dEsC")));
    }

    @Test
    public void testFromDummyString() {
        assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.of("dummy")));
    }
}
