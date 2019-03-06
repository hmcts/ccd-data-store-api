package uk.gov.hmcts.ccd.data.casedetails.search;

import java.util.Optional;

import org.junit.Test;

import org.junit.Assert;

public class SearchDirectionTest {

    @Test
    public void test_from_null_string() {
    	Assert.assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.empty()));
    }

    @Test
    public void test_from_asc_case_insensitive() {
    	Assert.assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.of("AsC")));
    }

    @Test
    public void test_from_desc_case_insensitive() {
    	Assert.assertEquals(SortDirection.DESC, SortDirection.fromOptionalString(Optional.of("dEsC")));
    }

    @Test
    public void test_from_dummy_string() {
    	Assert.assertEquals(SortDirection.ASC, SortDirection.fromOptionalString(Optional.of("dummy")));
    }
}
