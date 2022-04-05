package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity.STANDARD_LINK;

@ExtendWith(MockitoExtension.class)
class CaseLinkServiceTest extends CaseLinkTestFixtures {

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseLinkMapper caseLinkMapper;

    @InjectMocks
    private CaseLinkService caseLinkService;

    @Test
    void findCaseLinksReturnsNoResults() {

        // GIVEN
        when(caseLinkRepository.findAllByCaseReference(CASE_REFERENCE)).thenReturn(Collections.emptyList());

        // WHEN
        final List<CaseLink> result = caseLinkService.findCaseLinks(CASE_REFERENCE.toString());

        // THEN
        assertTrue(result.isEmpty());
        verify(caseLinkRepository).findAllByCaseReference(CASE_REFERENCE);
    }

    @Test
    void findCaseLinksReturnsResultIncludingLinkedCaseReferenceValuesFromLookup() {

        // GIVEN
        List<CaseLinkEntity> caseLinkEntities = List.of(
            createCaseLinkEntity(LINKED_CASE_DATA_ID_01, STANDARD_LINK),
            createCaseLinkEntity(LINKED_CASE_DATA_ID_02, NON_STANDARD_LINK),
            createCaseLinkEntity(LINKED_CASE_DATA_ID_03, NON_STANDARD_LINK),
            createCaseLinkEntity(LINKED_CASE_DATA_ID_04, STANDARD_LINK)
        );
        when(caseLinkRepository.findAllByCaseReference(CASE_REFERENCE)).thenReturn(caseLinkEntities);

        when(caseLinkMapper.entitiesToModels(caseLinkEntities)).thenReturn(
            List.of(
                // NB: set linked case references to null to prove they have been adjusted by lookup
                createCaseLink(null, LINKED_CASE_DATA_ID_01, STANDARD_LINK),
                createCaseLink(null, LINKED_CASE_DATA_ID_02, NON_STANDARD_LINK),
                createCaseLink(null, LINKED_CASE_DATA_ID_03, NON_STANDARD_LINK),
                createCaseLink(null, LINKED_CASE_DATA_ID_04, STANDARD_LINK)
            )
        );

        // setup lookups
        setupMockForCaseDetailsRepositoryFindById(LINKED_CASE_DATA_ID_01, LINKED_CASE_REFERENCE_01);
        setupMockForCaseDetailsRepositoryFindById(LINKED_CASE_DATA_ID_02, LINKED_CASE_REFERENCE_02);
        setupMockForCaseDetailsRepositoryFindById(LINKED_CASE_DATA_ID_03, LINKED_CASE_REFERENCE_03);
        setupMockForCaseDetailsRepositoryFindById(LINKED_CASE_DATA_ID_04, LINKED_CASE_REFERENCE_04);

        // WHEN
        final List<CaseLink> result = caseLinkService.findCaseLinks(CASE_REFERENCE.toString());

        // THEN
        assertEquals(4, result.size());

        // verify lookups used
        verify(caseDetailsRepository, times(4)).findById(isNull(), anyLong());
        verify(caseDetailsRepository).findById(null, LINKED_CASE_DATA_ID_01);
        verify(caseDetailsRepository).findById(null, LINKED_CASE_DATA_ID_02);
        verify(caseDetailsRepository).findById(null, LINKED_CASE_DATA_ID_03);
        verify(caseDetailsRepository).findById(null, LINKED_CASE_DATA_ID_04);

        assertCaseLink(result, LINKED_CASE_REFERENCE_01, STANDARD_LINK);
        assertCaseLink(result, LINKED_CASE_REFERENCE_02, NON_STANDARD_LINK);
        assertCaseLink(result, LINKED_CASE_REFERENCE_03, NON_STANDARD_LINK);
        assertCaseLink(result, LINKED_CASE_REFERENCE_04, STANDARD_LINK);
    }

    @Test
    void updateCaseLinksCanCreateACaseLinkRecord() {

        final List<CaseLink> caseLinks = List.of(
            createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK)
        );

        caseLinkService.updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);

        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            CASE_REFERENCE,
            LINKED_CASE_REFERENCE_01,
            CASE_TYPE_ID,
            STANDARD_LINK
        );
    }

    @Test
    void updateCaseLinksCanCreateMultipleCaseLinkRecordsWithCorrectStandardCaseLinkFlag() {

        // GIVEN
        List<CaseLink> caseLinks = List.of(
            createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK),
            createCaseLink(LINKED_CASE_REFERENCE_02, LINKED_CASE_DATA_ID_02, NON_STANDARD_LINK),
            createCaseLink(LINKED_CASE_REFERENCE_03, LINKED_CASE_DATA_ID_03, NON_STANDARD_LINK),
            createCaseLink(LINKED_CASE_REFERENCE_04, LINKED_CASE_DATA_ID_04, STANDARD_LINK)
        );

        // WHEN
        caseLinkService.updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);

        // THEN
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            CASE_REFERENCE,
            LINKED_CASE_REFERENCE_01,
            CASE_TYPE_ID,
            STANDARD_LINK
        );
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            CASE_REFERENCE,
            LINKED_CASE_REFERENCE_02,
            CASE_TYPE_ID,
            NON_STANDARD_LINK
        );
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            CASE_REFERENCE,
            LINKED_CASE_REFERENCE_03,
            CASE_TYPE_ID,
            NON_STANDARD_LINK
        );
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            CASE_REFERENCE,
            LINKED_CASE_REFERENCE_04,
            CASE_TYPE_ID,
            STANDARD_LINK
        );
    }

    @Test
    void updateCaseLinksWillFiltersOutCaseLinksThatAreNullOrBlanks() {

        // GIVEN
        final List<CaseLink> validCaseLinks = List.of(
            createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK),
            createCaseLink(LINKED_CASE_REFERENCE_02, LINKED_CASE_DATA_ID_02, NON_STANDARD_LINK)
        );
        final List<CaseLink> allCaseLinks = new ArrayList<>(validCaseLinks);
        allCaseLinks.add(null);
        allCaseLinks.add(CaseLink.builder().build());

        // WHEN
        caseLinkService.updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, allCaseLinks);

        // THEN
        verify(caseLinkRepository, times(2)).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            anyLong(),
            anyLong(),
            anyString(),
            anyBoolean()
        );
    }

    @Test
    void updateCaseLinksDeletesCaseLinkFields() {

        // GIVEN
        List<CaseLink> caseLinks = List.of(
            createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK)
        );

        // WHEN
        caseLinkService.updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);

        // THEN
        verify(caseLinkRepository).deleteAllByCaseReference(CASE_REFERENCE);
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            eq(CASE_REFERENCE),
            anyLong(),
            eq(CASE_TYPE_ID),
            anyBoolean()
        );
    }

    @Test
    void updateCaseLinksDeletesAllCaseLinksBeforeCreatingNew() {

        // GIVEN
        List<CaseLink> caseLinks = List.of(
            createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK)
        );

        // WHEN
        caseLinkService.updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);

        // THEN
        InOrder inOrder = Mockito.inOrder(caseLinkRepository);
        inOrder.verify(caseLinkRepository).deleteAllByCaseReference(CASE_REFERENCE);
        inOrder.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
            CASE_REFERENCE,
            LINKED_CASE_REFERENCE_01,
            CASE_TYPE_ID,
            STANDARD_LINK
        );
    }

    private CaseLink createCaseLink(Long linkedCaseReference, Long caseDataId, Boolean isStandardLink) {
        return CaseLink.builder()
            .caseReference(CASE_REFERENCE)
            .caseId(CASE_DATA_ID)
            .linkedCaseReference(linkedCaseReference)
            .linkedCaseId(caseDataId)
            .standardLink(isStandardLink)
            .build();
    }

    private CaseLinkEntity createCaseLinkEntity(Long caseDataId, Boolean isStandardLink) {
        return  new CaseLinkEntity(CASE_DATA_ID, caseDataId, CASE_TYPE_ID, isStandardLink);
    }

    private void setupMockForCaseDetailsRepositoryFindById(Long linkedCaseDataId01, Long linkedCaseReference01) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(linkedCaseDataId01.toString());
        caseDetails.setReference(linkedCaseReference01);
        when(caseDetailsRepository.findById(null, linkedCaseDataId01)).thenReturn(Optional.of(caseDetails));
    }

}
