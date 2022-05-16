package uk.gov.hmcts.ccd.domain.service.caselinking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.STANDARD_LINK;

@ExtendWith(MockitoExtension.class)
class CaseLinkServiceTest extends CaseLinkTestFixtures {

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseLinkExtractor caseLinkExtractor;

    @Mock
    private CaseLinkMapper caseLinkMapper;

    @InjectMocks
    private CaseLinkService caseLinkService;

    @Nested
    @DisplayName("findCaseLinks")
    class FindCaseLinks {

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

        private CaseLinkEntity createCaseLinkEntity(Long caseDataId, Boolean isStandardLink) {
            return  new CaseLinkEntity(CASE_DATA_ID, caseDataId, CASE_TYPE_ID, isStandardLink);
        }

        private void setupMockForCaseDetailsRepositoryFindById(Long linkedCaseDataId, Long linkedCaseReference) {
            CaseDetails caseDetails = new CaseDetails();
            caseDetails.setId(linkedCaseDataId.toString());
            caseDetails.setReference(linkedCaseReference);
            when(caseDetailsRepository.findById(null, linkedCaseDataId)).thenReturn(Optional.of(caseDetails));
        }
    }


    @Nested
    @DisplayName("updateCaseLinks")
    class UpdateCaseLinks {

        private final CaseDetails caseDetails = createCaseDetails(null);
        private final CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition();

        @Test
        void updateCaseLinksCanCreateACaseLinkRecord() {

            // GIVEN
            final List<CaseLink> caseLinks = List.of(
                createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK)
            );
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(caseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_01,
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
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(caseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_01,
                STANDARD_LINK
            );
            verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_02,
                NON_STANDARD_LINK
            );
            verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_03,
                NON_STANDARD_LINK
            );
            verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_04,
                STANDARD_LINK
            );
        }

        @Test
        void updateCaseLinksWillFilterOutCaseLinksThatAreNullOrBlanks() {

            // GIVEN
            final List<CaseLink> validCaseLinks = List.of(
                createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK),
                createCaseLink(LINKED_CASE_REFERENCE_02, LINKED_CASE_DATA_ID_02, NON_STANDARD_LINK)
            );
            final List<CaseLink> allCaseLinks = new ArrayList<>(validCaseLinks);
            allCaseLinks.add(null);
            allCaseLinks.add(CaseLink.builder().build());
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(allCaseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository, times(2)).insertUsingCaseReferences(
                anyLong(),
                anyLong(),
                anyBoolean()
            );
        }

        @Test
        void updateCaseLinksDeletesCaseLinkFields() {

            // GIVEN
            List<CaseLink> caseLinks = List.of(
                createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK)
            );
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(caseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository).deleteAllByCaseReference(CASE_REFERENCE);
            verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_01,
                STANDARD_LINK
            );
        }

        @Test
        void updateCaseLinksDeletesAllCaseLinksBeforeCreatingNew() {

            // GIVEN
            List<CaseLink> caseLinks = List.of(
                createCaseLink(LINKED_CASE_REFERENCE_01, LINKED_CASE_DATA_ID_01, STANDARD_LINK)
            );
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(caseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            InOrder inOrder = Mockito.inOrder(caseLinkRepository);
            inOrder.verify(caseLinkRepository).deleteAllByCaseReference(CASE_REFERENCE);
            inOrder.verify(caseLinkRepository).insertUsingCaseReferences(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_01,
                STANDARD_LINK
            );
        }

    }

    private CaseLink createCaseLink(Long linkedCaseReference, Long linkedCaseDataId, Boolean isStandardLink) {
        return CaseLink.builder()
            .caseReference(CASE_REFERENCE)
            .caseId(CASE_DATA_ID)
            .linkedCaseReference(linkedCaseReference)
            .linkedCaseId(linkedCaseDataId)
            .standardLink(isStandardLink)
            .build();
    }

}
