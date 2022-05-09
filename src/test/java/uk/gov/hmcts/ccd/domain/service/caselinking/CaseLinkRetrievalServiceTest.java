package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.STANDARD_LINK;
import static uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkExtractor.STANDARD_CASE_LINK_FIELD;

@ExtendWith(MockitoExtension.class)
class CaseLinkRetrievalServiceTest extends CaseLinkTestFixtures {

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @Mock
    private GetCaseOperation getCaseOperation;

    @InjectMocks
    private CaseLinkRetrievalService caseLinkRetrievalService;

    private static final Long LINKED_CASE_REFERENCE_05 = 8915783755360086L;
    private static final Long LINKED_CASE_REFERENCE_06 = 2838768175385992L;

    @Test
    void testGetStandardLinkedCasesNoPagination() {

        // GIVEN
        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_01,
            LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03);

        mockCaseLinkRepositoryCalls(linkedCaseReferences);
        mockGetCaseOperationCalls(linkedCaseReferences);

        // WHEN
        final CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE.toString(), 1, 0);

        // THEN
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

        // GIVEN
        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_01,
            LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04, LINKED_CASE_REFERENCE_05);

        mockCaseLinkRepositoryCalls(linkedCaseReferences);
        mockGetCaseOperationCalls(linkedCaseReferences);

        // WHEN
        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE.toString(), 1, 4);

        // THEN
        final List<Long> expectedLinkedCases = List.of(LINKED_CASE_REFERENCE_01, LINKED_CASE_REFERENCE_02,
            LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04);
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

        // GIVEN
        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_01,
            LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04, LINKED_CASE_REFERENCE_05,
            LINKED_CASE_REFERENCE_06);

        mockCaseLinkRepositoryCalls(linkedCaseReferences);
        mockGetCaseOperationCalls(linkedCaseReferences);

        // WHEN
        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE.toString(), 4, 3);

        // THEN
        final List<Long> expectedLinkedCases = List.of(LINKED_CASE_REFERENCE_04, LINKED_CASE_REFERENCE_05,
            LINKED_CASE_REFERENCE_06);
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

        // GIVEN
        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_01,
            LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04, LINKED_CASE_REFERENCE_05,
            LINKED_CASE_REFERENCE_06);

        mockCaseLinkRepositoryCalls(linkedCaseReferences);
        mockGetCaseOperationCalls(linkedCaseReferences);

        // WHEN / THEN
        assertPageResults(1, List.of(LINKED_CASE_REFERENCE_01, LINKED_CASE_REFERENCE_02), true);
        assertPageResults(3, List.of(LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04), true);
        assertPageResults(5, List.of(LINKED_CASE_REFERENCE_05, LINKED_CASE_REFERENCE_06), false);
    }

    private void assertPageResults(int startRecordNumber, List<Long> expectedLinkedCases, boolean hasMoreResults) {

        // WHEN
        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE.toString(), startRecordNumber, 2);

        // THEN
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

        // WHEN
        final CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE.toString(), 1, 0);

        // THEN
        assertTrue(standardLinkedCases.getCaseDetails().isEmpty());
        assertFalse(standardLinkedCases.isHasMoreResults());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testShouldExcludeCasesThatDoNotReturnTheStandardCaseLinkField() {

        // GIVEN
        Long caseReferenceToSkip = CASE_REFERENCE_02;
        List<Long> linkedCaseReferences = List.of(LINKED_CASE_REFERENCE_01,
            LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03, caseReferenceToSkip, LINKED_CASE_REFERENCE_04);

        mockCaseLinkRepositoryCalls(linkedCaseReferences);
        // NB: mock caseReferenceToSkip separately
        mockGetCaseOperationCalls(List.of(LINKED_CASE_REFERENCE_01,
            LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04));
        // mock caseDetailsToSkip without the standard caseLink field
        CaseDetails caseDetailsToSkip = createCaseDetails(caseReferenceToSkip, List.of());
        when(getCaseOperation.execute(valueOf(caseReferenceToSkip)))
            .thenReturn(Optional.of(caseDetailsToSkip));

        // WHEN
        CaseLinkRetrievalResults standardLinkedCases =
            caseLinkRetrievalService.getStandardLinkedCases(CASE_REFERENCE.toString(), 1, 4);

        // THEN
        final List<Long> expectedLinkedCases = List.of(LINKED_CASE_REFERENCE_01, LINKED_CASE_REFERENCE_02,
            LINKED_CASE_REFERENCE_03, LINKED_CASE_REFERENCE_04);
        assertEquals(expectedLinkedCases.size(), standardLinkedCases.getCaseDetails().size());
        final List<Long> collect = standardLinkedCases.getCaseDetails()
            .stream()
            .map(CaseDetails::getId)
            .map(Long::parseLong)
            .collect(Collectors.toList());

        assertTrue(collect.containsAll(expectedLinkedCases));
        assertFalse(standardLinkedCases.isHasMoreResults());

        // confirm case with standard caseLink field that is hidden from user is excluded from results
        assertFalse(collect.contains(caseReferenceToSkip));
    }

    private CaseDetails createCaseDetails(Long linkedCaseReference, List<String> dataValues) {
        CaseDetails caseDetails = null;

        try {
            caseDetails = createCaseDetails(linkedCaseReference, CASE_TYPE_ID, createCaseDataMap(dataValues));
            caseDetails.setId(valueOf(linkedCaseReference));

        } catch (JsonProcessingException e) {
            fail("Unexpected exception when prepping mock data: " + e.getMessage());
        }

        return caseDetails;
    }

    private void mockGetCaseOperationCalls(List<Long> linkedCaseReferences) {
        linkedCaseReferences.forEach(linkedCaseReference -> {
            CaseDetails caseDetails = createCaseDetails(
                linkedCaseReference,
                List.of(
                    createCaseLinkCollectionString(
                        STANDARD_CASE_LINK_FIELD,
                        List.of(
                            CASE_REFERENCE_02.toString(), // i.e. another linked case to be ignored
                            CASE_REFERENCE.toString()
                        )
                    )
                )
            );

            when(getCaseOperation.execute(valueOf(linkedCaseReference)))
                .thenReturn(Optional.of(caseDetails));
        });
    }

    private void mockCaseLinkRepositoryCalls(List<Long> linkedCaseReferences) {
        when(caseLinkRepository.findCaseReferencesByLinkedCaseReferenceAndStandardLink(CASE_REFERENCE, STANDARD_LINK))
            .thenReturn(linkedCaseReferences);
    }


}
