package uk.gov.hmcts.ccd.domain.service.caselinking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.STANDARD_LINK;

@ExtendWith(MockitoExtension.class)
class CaseLinkRetrievalServiceTest {

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @Mock
    private GetCaseOperation getCaseOperation;

    @InjectMocks
    private CaseLinkRetrievalService caseLinkRetrievalService;

    private static final String CASE_REFERENCE = "1500638105106660";

    private static final Long LINKED_CASE_REFERENCE_1 = 1500638105106660L;
    private static final Long LINKED_CASE_REFERENCE_2 = 9514840069336542L;
    private static final Long LINKED_CASE_REFERENCE_3 = 4827897342988773L;
    private static final Long LINKED_CASE_REFERENCE_4 = 6347307120125883L;
    private static final Long LINKED_CASE_REFERENCE_5 = 8915783755360086L;
    private static final Long LINKED_CASE_REFERENCE_6 = 2838768175385992L;

    @Test
    void testGetStandardLinkedCasesNoPagination() {

        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_1,
            LINKED_CASE_REFERENCE_2, LINKED_CASE_REFERENCE_3);
        when(caseLinkRepository.findAllByCaseReferenceAndStandardLink(parseLong(CASE_REFERENCE), STANDARD_LINK))
            .thenReturn(linkedCaseReferences);

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(valueOf(LINKED_CASE_REFERENCE_1));

        CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setId(valueOf(LINKED_CASE_REFERENCE_2));

        CaseDetails caseDetails3 = new CaseDetails();
        caseDetails3.setId(valueOf(LINKED_CASE_REFERENCE_3));

        when(getCaseOperation.execute(valueOf(LINKED_CASE_REFERENCE_1))).thenReturn(Optional.of(caseDetails));
        when(getCaseOperation.execute(valueOf(LINKED_CASE_REFERENCE_2))).thenReturn(Optional.of(caseDetails2));
        when(getCaseOperation.execute(valueOf(LINKED_CASE_REFERENCE_3))).thenReturn(Optional.of(caseDetails3));

        final CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE, 1, 0);

        assertEquals(linkedCaseReferences.size(), standardLinkedCases.getCaseDetails().size());
        final List<Long> collect = standardLinkedCases.getCaseDetails()
            .stream()
            .map(CaseDetails::getId)
            .map(Long::parseLong)
            .collect(Collectors.toList());
        assertTrue(collect.containsAll(linkedCaseReferences));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testGetStandardLinkedCasesGetFirstHalfOfResults() {

        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_1,
            LINKED_CASE_REFERENCE_2, LINKED_CASE_REFERENCE_3, LINKED_CASE_REFERENCE_4, LINKED_CASE_REFERENCE_5,
            LINKED_CASE_REFERENCE_6);

        when(caseLinkRepository.findAllByCaseReferenceAndStandardLink(parseLong(CASE_REFERENCE), STANDARD_LINK))
            .thenReturn(linkedCaseReferences);

        linkedCaseReferences.forEach(linkedCaseReference -> {
            CaseDetails caseDetails = new CaseDetails();
            caseDetails.setId(valueOf(linkedCaseReference));
            when(getCaseOperation.execute(valueOf(linkedCaseReference)))
                .thenReturn(Optional.of(caseDetails));
        });

        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE, 1, 4);

        final List<Long> expectedLinkedCases = List.of(LINKED_CASE_REFERENCE_1, LINKED_CASE_REFERENCE_2,
            LINKED_CASE_REFERENCE_3, LINKED_CASE_REFERENCE_4);
        assertEquals(expectedLinkedCases.size(), standardLinkedCases.getCaseDetails().size());
        final List<Long> collect = standardLinkedCases.getCaseDetails()
            .stream()
            .map(CaseDetails::getId)
            .map(Long::parseLong)
            .collect(Collectors.toList());

        assertTrue(collect.containsAll(expectedLinkedCases));
        assertTrue(standardLinkedCases.isHasMoreResults());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testGetStandardLinkedCasesGetSecondHalfOfResults() {

        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_1,
            LINKED_CASE_REFERENCE_2, LINKED_CASE_REFERENCE_3, LINKED_CASE_REFERENCE_4, LINKED_CASE_REFERENCE_5,
            LINKED_CASE_REFERENCE_6);

        when(caseLinkRepository.findAllByCaseReferenceAndStandardLink(parseLong(CASE_REFERENCE), STANDARD_LINK))
            .thenReturn(linkedCaseReferences);

        linkedCaseReferences.stream().forEach(linkedCaseReference -> {
            CaseDetails caseDetails = new CaseDetails();
            caseDetails.setId(valueOf(linkedCaseReference));
            when(getCaseOperation.execute(valueOf(linkedCaseReference)))
                .thenReturn(Optional.of(caseDetails));
        });

        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE, 4, 3);

        final List<Long> expectedLinkedCases = List.of(LINKED_CASE_REFERENCE_4, LINKED_CASE_REFERENCE_5,
            LINKED_CASE_REFERENCE_6);
        assertEquals(expectedLinkedCases.size(), standardLinkedCases.getCaseDetails().size());
        final List<Long> caseDetailIds = standardLinkedCases.getCaseDetails()
            .stream()
            .map(CaseDetails::getId)
            .map(Long::parseLong)
            .collect(Collectors.toList());

        assertTrue(caseDetailIds.containsAll(expectedLinkedCases));
        assertFalse(standardLinkedCases.isHasMoreResults());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testGetStandardLinkedCasesThreePagesOfResults() {

        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_1,
            LINKED_CASE_REFERENCE_2, LINKED_CASE_REFERENCE_3, LINKED_CASE_REFERENCE_4, LINKED_CASE_REFERENCE_5,
            LINKED_CASE_REFERENCE_6);

        when(caseLinkRepository.findAllByCaseReferenceAndStandardLink(parseLong(CASE_REFERENCE), STANDARD_LINK))
            .thenReturn(linkedCaseReferences);

        linkedCaseReferences.stream().forEach(linkedCaseReference -> {
            CaseDetails caseDetails = new CaseDetails();
            caseDetails.setId(valueOf(linkedCaseReference));
            when(getCaseOperation.execute(valueOf(linkedCaseReference)))
                .thenReturn(Optional.of(caseDetails));
        });

        assertPageResults(1, List.of(LINKED_CASE_REFERENCE_1, LINKED_CASE_REFERENCE_2), true);
        assertPageResults(3, List.of(LINKED_CASE_REFERENCE_3, LINKED_CASE_REFERENCE_4), true);
        assertPageResults(5, List.of(LINKED_CASE_REFERENCE_5, LINKED_CASE_REFERENCE_6), false);
    }

    private void assertPageResults(int startRecordNumber, List<Long> expectedLinkedCases, boolean hasMoreResults) {
        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE, startRecordNumber, 2);

        assertEquals(expectedLinkedCases.size(), standardLinkedCases.getCaseDetails().size());

        List<Long> caseDetailIds = standardLinkedCases.getCaseDetails()
            .stream()
            .map(CaseDetails::getId)
            .map(Long::parseLong)
            .collect(Collectors.toList());

        assertTrue(caseDetailIds.containsAll(expectedLinkedCases));

        if (hasMoreResults) {
            assertTrue(standardLinkedCases.isHasMoreResults());
        } else {
            assertFalse(standardLinkedCases.isHasMoreResults());
        }

    }

    @Test
    void testGetStandardLinkedCasesNoCasesFound() {

        final CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE, 1, 0);
        assertTrue(standardLinkedCases.getCaseDetails().isEmpty());
        assertFalse(standardLinkedCases.isHasMoreResults());
    }
}
