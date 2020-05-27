package uk.gov.hmcts.ccd.domain.model.search;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CaseSearchResultTest {

    private static final String CASE_TYPE_A = "CaseTypeA";
    private static final String CASE_TYPE_B = "CaseTypeB";

    @Test
    void shouldBuildCaseReferenceListForProvidedCaseType() {
        List<CaseDetails> cases = new ArrayList<>();
        cases.add(caseDetails(CASE_TYPE_A, 111L));
        cases.add(caseDetails(CASE_TYPE_B, 222L));
        cases.add(caseDetails(CASE_TYPE_A, 333L));
        cases.add(caseDetails(CASE_TYPE_B, 444L));
        cases.add(caseDetails(CASE_TYPE_A, 555L));
        CaseSearchResult caseSearchResult = new CaseSearchResult(5L, cases);

        final List<String> result = caseSearchResult.buildCaseReferenceList(CASE_TYPE_A);

        assertAll(
            () -> assertThat(result.size(), is(3)),
            () -> assertTrue(result.contains("111")),
            () -> assertTrue(result.contains("333")),
            () -> assertTrue(result.contains("555"))
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoCasesFound() {
        CaseSearchResult caseSearchResult = new CaseSearchResult(1L, Collections.singletonList(caseDetails(CASE_TYPE_A, 111L)));

        final List<String> result = caseSearchResult.buildCaseReferenceList(CASE_TYPE_B);

        assertAll(
            () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void shouldReturnEmptyListWhenCasesIsNull() {
        CaseSearchResult caseSearchResult = new CaseSearchResult();

        final List<String> result = caseSearchResult.buildCaseReferenceList(CASE_TYPE_A);

        assertAll(
            () -> assertTrue(result.isEmpty())
        );
    }

    private CaseDetails caseDetails(String caseTypeId, Long reference) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setReference(reference);
        return caseDetails;
    }
}
