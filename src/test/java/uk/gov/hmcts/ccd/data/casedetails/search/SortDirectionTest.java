package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class SortDirectionTest {

    @Test
    public void testFromNullString() {
        Assert.assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.empty()));
    }

    @Test
    public void testFromAscCaseInsensitive() {
        Assert.assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.of("AsC")));
    }

    @Test
    public void testFromDescCaseInsensitive() {
        Assert.assertEquals(SortDirection.DESC, SortDirection.fromOptionalString(Optional.of("dEsC")));
    }

    @Test
    public void testFromDummyString() {
        Assert.assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.of("dummy")));
    }
}
