package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseSearchResultViewTest {

    private static final String JURISDICTION = "Jurisdiction";
    private static final String CASE_TYPE = "CaseTypeA";

    @Test
    void shouldFindHeaderByCaseType() {
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup correctHeader =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup(
            new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata(JURISDICTION, CASE_TYPE),
                        emptyList(), emptyList()
        );
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup otherHeader =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup(
                        new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata(JURISDICTION,
                                "Other Case Type"), emptyList(), emptyList());
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView caseSearchResultView =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView(
                        Arrays.asList(otherHeader, correctHeader), emptyList(), 0L);

        Optional<uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup> result =
                caseSearchResultView.findHeaderByCaseType(CASE_TYPE);

        assertAll(
            () -> assertTrue(result.isPresent()),
            () -> assertThat(result.get(), is(correctHeader))
        );
    }

    @Test
    void shouldNotFindNonExistingHeader() {
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup header =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup(
            new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata(JURISDICTION, CASE_TYPE),
                        emptyList(), emptyList()
        );
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView caseSearchResultView =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView(
                        Collections.singletonList(header), emptyList(), 0L);

        Optional<uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup> result =
                caseSearchResultView.findHeaderByCaseType("Other Case Type");

        assertAll(
            () -> assertFalse(result.isPresent())
        );
    }

    @Test
    void shouldFindCaseByReference() {
        SearchResultViewItem item1 = new SearchResultViewItem("111", emptyMap(), emptyMap(), emptyMap());
        SearchResultViewItem item2 = new SearchResultViewItem("222", emptyMap(), emptyMap(), emptyMap());
        CaseSearchResultView caseSearchResultView =
                new CaseSearchResultView(emptyList(), Arrays.asList(item1, item2), 0L);

        Optional<SearchResultViewItem> result = caseSearchResultView.findCaseByReference("222");

        assertAll(
            () -> assertTrue(result.isPresent()),
            () -> assertThat(result.get(), is(item2))
        );
    }

    @Test
    void shouldNotFindNonExistingCase() {
        SearchResultViewItem item = new SearchResultViewItem("111", emptyMap(), emptyMap(), emptyMap());
        CaseSearchResultView caseSearchResultView =
                new CaseSearchResultView(emptyList(), Collections.singletonList(item), 0L);

        Optional<SearchResultViewItem> result = caseSearchResultView.findCaseByReference("000");

        assertAll(
            () -> assertFalse(result.isPresent())
        );
    }

    @Test
    void shouldFindCasesByCaseType() {
        SearchResultViewItem correctItem1 = new SearchResultViewItem("111", emptyMap(), emptyMap(), emptyMap());
        SearchResultViewItem correctItem2 = new SearchResultViewItem("222", emptyMap(), emptyMap(), emptyMap());
        SearchResultViewItem otherItem3 = new SearchResultViewItem("333", emptyMap(), emptyMap(), emptyMap());
        SearchResultViewHeaderGroup correctHeader = new SearchResultViewHeaderGroup(
            new HeaderGroupMetadata(JURISDICTION, CASE_TYPE), emptyList(), Arrays.asList("111", "222")
        );
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup otherHeader =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup(
            new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata(JURISDICTION,
                    "Other Case Type"), emptyList(), Arrays.asList("333")
        );
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView caseSearchResultView =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView(
            Arrays.asList(otherHeader, correctHeader), Arrays.asList(correctItem1, otherItem3, correctItem2), 0L
        );

        List<SearchResultViewItem> result = caseSearchResultView.findCasesByCaseType(CASE_TYPE);

        assertAll(
            () -> assertThat(result.size(), is(2)),
            () -> assertThat(result, hasItem(correctItem1)),
            () -> assertThat(result, hasItem(correctItem2))
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoCasesForCaseType() {
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup header =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup(
            new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata(JURISDICTION, CASE_TYPE),
                        emptyList(), emptyList()
        );
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView caseSearchResultView =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView(singletonList(header),
                        emptyList(), 0L);

        List<SearchResultViewItem> result = caseSearchResultView.findCasesByCaseType(CASE_TYPE);

        assertAll(
            () -> assertThat(result.size(), is(0))
        );
    }
}
