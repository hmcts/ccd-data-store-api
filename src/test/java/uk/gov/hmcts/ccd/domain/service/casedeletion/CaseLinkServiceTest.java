package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
                createCaseLinkEntity(LINKED_CASE_DATA_ID_01),
                createCaseLinkEntity(LINKED_CASE_DATA_ID_02),
                createCaseLinkEntity(LINKED_CASE_DATA_ID_03),
                createCaseLinkEntity(LINKED_CASE_DATA_ID_04)
            );
            when(caseLinkRepository.findAllByCaseReference(CASE_REFERENCE)).thenReturn(caseLinkEntities);

            when(caseLinkMapper.entitiesToModels(caseLinkEntities)).thenReturn(
                List.of(
                    // NB: set linked case references to null to prove they have been adjusted by lookup
                    createCaseLink(null, LINKED_CASE_DATA_ID_01),
                    createCaseLink(null, LINKED_CASE_DATA_ID_02),
                    createCaseLink(null, LINKED_CASE_DATA_ID_03),
                    createCaseLink(null, LINKED_CASE_DATA_ID_04)
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

            assertCaseLink(result, LINKED_CASE_REFERENCE_01);
            assertCaseLink(result, LINKED_CASE_REFERENCE_02);
            assertCaseLink(result, LINKED_CASE_REFERENCE_03);
            assertCaseLink(result, LINKED_CASE_REFERENCE_04);
        }

        private CaseLinkEntity createCaseLinkEntity(Long caseDataId) {
            return  new CaseLinkEntity(CASE_DATA_ID, caseDataId, CASE_TYPE_ID);
        }
    }

    @Nested
    @DisplayName("updateCaseLinks")
    class UpdateCaseLinks {

        private final CaseDetails caseDetails = createCaseDetails(null);
        private final CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition();

        @Test
        void updateCaseLinksDeletesAndInsertsCaseLinkFields() {
            // SCENARIO:  BEFORE 02, 03, AFTER: 01, 02

            // GIVEN
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(List.of(
                    LINKED_CASE_REFERENCE_01.toString(),
                    LINKED_CASE_REFERENCE_02.toString()
                ));

            mockCallsToFindCurrentCaseLinksToCase02AndCase03();

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_03 // i.e. existing linked case 03 has been removed
            );
            verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_01, // i.e. new linked case 01 has been added
                CASE_TYPE_ID
            );
            verifyNoMoreInteractions(caseLinkRepository); // i.e. existing linked case 02 is unchanged
        }

        @Test
        void updateCaseLinksDeletesCaseLinkFields() {
            // SCENARIO:  BEFORE 02, 03, AFTER: none

            // GIVEN
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(List.of());

            mockCallsToFindCurrentCaseLinksToCase02AndCase03();

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository, times(2)).deleteByCaseReferenceAndLinkedCaseReference(
                anyLong(),
                anyLong()
            );
            verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_02 // i.e. existing linked case 02 has been removed
            );
            verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_03 // i.e. existing linked case 03 has been removed
            );
            verify(caseLinkRepository, never()).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
                anyLong(),
                anyLong(),
                anyString()
            );
        }

        @Test
        void updateCaseLinksInsertsCaseLinkFields() {
            // SCENARIO:  BEFORE none, AFTER: 01, 02

            // GIVEN
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(List.of(
                    LINKED_CASE_REFERENCE_01.toString(),
                    LINKED_CASE_REFERENCE_02.toString()
                ));

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_01, // i.e. new linked case 01 has been added
                CASE_TYPE_ID
            );
            verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
                CASE_REFERENCE,
                LINKED_CASE_REFERENCE_02, // i.e. new linked case 02 has been added
                CASE_TYPE_ID
            );
        }

        @Test
        void updateCaseLinksNoRepositoryInteractionCaseLinksIdentical() {
            // SCENARIO:  BEFORE: 02, 03, AFTER: 02, 03 (i.e. unchanged)

            // GIVEN
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(List.of(
                    LINKED_CASE_REFERENCE_02.toString(),
                    LINKED_CASE_REFERENCE_03.toString()
                ));

            mockCallsToFindCurrentCaseLinksToCase02AndCase03();

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            verify(caseLinkRepository, never()).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
                anyLong(),
                anyLong(),
                anyString()
            );
            verify(caseLinkRepository, never()).deleteByCaseReferenceAndLinkedCaseReference(
                anyLong(),
                anyLong()
            );
        }

        @Test
        void createCaseLinks() {
            // SCENARIO:  BEFORE: none, AFTER: 01, 02

            // GIVEN
            final List<String> caseLinks = List.of(
                LINKED_CASE_REFERENCE_01.toString(),
                LINKED_CASE_REFERENCE_02.toString()
            );
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(caseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            caseLinks.forEach(caseLink ->
                Mockito.verify(caseLinkRepository)
                    .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_REFERENCE,
                                                                              Long.valueOf(caseLink),
                                                                              CASE_TYPE_ID));
        }

        @Test
        void createCaseLinksFiltersOutNullOrBlanks() {

            // GIVEN
            final List<String> validCaseLinks = List.of(
                LINKED_CASE_REFERENCE_01.toString(),
                LINKED_CASE_REFERENCE_02.toString()
            );
            final List<String> allCaseLinks = new ArrayList<>(validCaseLinks);
            allCaseLinks.add(null);
            allCaseLinks.add("");
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(allCaseLinks);

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            validCaseLinks.forEach(caseLink ->
                Mockito.verify(caseLinkRepository)
                    .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(
                        CASE_REFERENCE,
                        Long.valueOf(caseLink),
                        CASE_TYPE_ID));
        }

        @Test
        void createCaseLinksNullAndBlankValuesNotInsertedToDB() {

            // GIVEN
            final List<String> caseLinks = new ArrayList<>();
            caseLinks.add(null);
            caseLinks.add("");
            caseLinks.add(null);
            when(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
                .thenReturn(List.of());

            // WHEN
            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());

            // THEN
            caseLinks.forEach(caseLink ->
                Mockito.verify(caseLinkRepository, never())
                    .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(anyLong(),
                        anyLong(),
                        anyString()));
        }

        private void mockCallsToFindCurrentCaseLinksToCase02AndCase03() {
            final List<CaseLinkEntity> caseLinkEntities = List.of(
                new CaseLinkEntity(CASE_DATA_ID, LINKED_CASE_DATA_ID_02, CASE_TYPE_ID),
                new CaseLinkEntity(CASE_DATA_ID, LINKED_CASE_DATA_ID_03, CASE_TYPE_ID)
            );

            final List<CaseLink> caseLinksModels = List.of(
                createCaseLink(LINKED_CASE_REFERENCE_02, LINKED_CASE_DATA_ID_02),
                createCaseLink(LINKED_CASE_REFERENCE_03, LINKED_CASE_DATA_ID_03)
            );

            when(caseLinkRepository.findAllByCaseReference(CASE_REFERENCE)).thenReturn(caseLinkEntities);
            when(caseLinkMapper.entitiesToModels(caseLinkEntities)).thenReturn(caseLinksModels);

            setupMockForCaseDetailsRepositoryFindById(LINKED_CASE_DATA_ID_02, LINKED_CASE_REFERENCE_02);
            setupMockForCaseDetailsRepositoryFindById(LINKED_CASE_DATA_ID_03, LINKED_CASE_REFERENCE_03);
        }
    }

    private void setupMockForCaseDetailsRepositoryFindById(Long linkedCaseDataId01, Long linkedCaseReference01) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(linkedCaseDataId01.toString());
        caseDetails.setReference(linkedCaseReference01);
        when(caseDetailsRepository.findById(null, linkedCaseDataId01)).thenReturn(Optional.of(caseDetails));
    }

    private CaseLink createCaseLink(Long linkedCaseReference, Long linkedCaseDataId) {
        return CaseLink.builder()
            .caseReference(CASE_REFERENCE)
            .caseId(CASE_DATA_ID)
            .linkedCaseReference(linkedCaseReference)
            .linkedCaseId(linkedCaseDataId)
            .build();
    }

}
