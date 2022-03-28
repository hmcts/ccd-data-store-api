package uk.gov.hmcts.ccd.domain.service.casedeletion;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_LIST;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkServiceTest {

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseLinkMapper caseLinkMapper;

    @InjectMocks
    private CaseLinkService caseLinkService;

    private static final String CASE_TYPE_ID = "Test";

    private static final long CASE_ID = 1L;

    @Test
    void updateCaseLinksDeletesAndInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(CASE_ID, "Test",
            createPreCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(CASE_ID, 1504259907353545L);
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                            1504259907353594L,
                                                                                            CASE_TYPE_ID);
    }

    @Test
    void updateCaseLinksDeletesCaseLinkFields() {
        caseLinkService.updateCaseLinks(CASE_ID, CASE_TYPE_ID,
            createPreCallbackCaseLinks(),
            EMPTY_LIST);

        verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(CASE_ID, 1504259907353545L);
        verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(CASE_ID, 1504259907353552L);
        verify(caseLinkRepository, never()).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                                    1504259907353594L,
                                                                                                    CASE_TYPE_ID);

    }

    @Test
    void updateCaseLinksInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(CASE_ID, CASE_TYPE_ID,
            EMPTY_LIST,
            createPostCallbackCaseLinks());

        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                            1504259907353552L,
                                                                                            CASE_TYPE_ID);
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                            1504259907353594L,
                                                                                            CASE_TYPE_ID);
    }

    @Test
    void updateCaseLinksNoRepositoryInteractionCaseLinksIdentical() {
        caseLinkService.updateCaseLinks(CASE_ID,
            CASE_TYPE_ID,
            createPostCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        verify(caseLinkRepository, never())
            .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(anyLong(), anyLong(), anyString());
        verify(caseLinkRepository, never()).deleteByCaseReferenceAndLinkedCaseReference(anyLong(), anyLong());
    }

    @Test
    void findCaseLinksReturnsNoResults() {
        final String caseReferenceToFind = "1504259907353545";
        final Long caseReferenceToFindLong = Long.parseLong(caseReferenceToFind);

        when(caseLinkRepository.findAllByCaseReference(caseReferenceToFindLong)).thenReturn(Collections.emptyList());

        final List<CaseLink> caseLinks = caseLinkService.findCaseLinks(caseReferenceToFind);
        assertTrue(caseLinks.isEmpty());
        verify(caseLinkRepository).findAllByCaseReference(caseReferenceToFindLong);
    }

    @Test
    void findCaseLinksReturnsSingleResult() {
        final String caseReferenceToFind = "1504259907353545";
        final Long caseReferenceToFindLong = Long.parseLong(caseReferenceToFind);
        final String caseTypeId = "Test";

        final Long caseId = 1L;
        final List<Long> linkedCaseIds = List.of(2L, 3L, 4L);
        final List<Long> linkedCaseReferences = List.of(1504259907353545L, 1504259907353545L, 1504259907353545L);
        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(caseId, linkedCaseIds.get(0), caseTypeId),
            new CaseLinkEntity(caseId, linkedCaseIds.get(1), caseTypeId),
            new CaseLinkEntity(caseId, linkedCaseIds.get(2), caseTypeId));

        final List<CaseLink> caseLinksModels = List.of(
            CaseLink.builder()
                .caseId(caseId)
                .linkedCaseId(linkedCaseIds.get(0))
                .caseTypeId(caseTypeId)
                .build(),
            CaseLink.builder()
                .caseId(caseId)
                .linkedCaseId(linkedCaseIds.get(1))
                .caseTypeId(caseTypeId)
                .build(),
            CaseLink.builder()
                .caseId(caseId)
                .linkedCaseId(linkedCaseIds.get(2))
                .caseTypeId(caseTypeId)
                .build()
        );

        when(caseLinkRepository.findAllByCaseReference(caseReferenceToFindLong)).thenReturn(caseLinkEntities);
        when(caseLinkMapper.entitiesToModels(caseLinkEntities)).thenReturn(caseLinksModels);
        when(caseDetailsRepository.findCaseReferencesByIds(linkedCaseIds)).thenReturn(linkedCaseReferences);

        final List<CaseLink> caseLinks = caseLinkService.findCaseLinks(caseReferenceToFind);
        assertFalse(caseLinks.isEmpty());

        final List<Long> foundLinkedCaseReferences = caseLinks.stream()
            .map(CaseLink::getLinkedCaseReference)
            .collect(Collectors.toList());

        assertTrue(foundLinkedCaseReferences.containsAll(linkedCaseReferences));
        verify(caseLinkRepository).findAllByCaseReference(caseReferenceToFindLong);
    }

    @Test
    void createCaseLinks() {

        final List<String> caseLinks = createPostCallbackCaseLinks();

        caseLinkService.createCaseLinks(CASE_ID, CASE_TYPE_ID, caseLinks);

        caseLinks.forEach(caseLink ->
            Mockito.verify(caseLinkRepository)
                .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                          Long.valueOf(caseLink),
                                                                          CASE_TYPE_ID));
    }

    @Test
    void createCaseLinksFiltersOutNullOrBlanks() {

        final List<String> validCaseLinks = createPostCallbackCaseLinks();
        final List<String> allCaseLinks = new ArrayList<>(validCaseLinks);
        allCaseLinks.add(null);
        allCaseLinks.add("");

        caseLinkService.createCaseLinks(CASE_ID, CASE_TYPE_ID, allCaseLinks);

        validCaseLinks.forEach(caseLink ->
            Mockito.verify(caseLinkRepository)
                .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                    Long.valueOf(caseLink),
                    CASE_TYPE_ID));
    }

    @Test
    void createCaseLinksNullAndBlankValuesNotInsertedToDB() {

        final List<String> caseLinks = new ArrayList<>();
        caseLinks.add(null);
        caseLinks.add("");
        caseLinks.add(null);

        caseLinkService.createCaseLinks(CASE_ID, CASE_TYPE_ID, caseLinks);

        caseLinks.forEach(caseLink ->
            Mockito.verify(caseLinkRepository, never())
                .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(anyLong(),
                    anyLong(),
                    anyString()));
    }

    private List<String> createPreCallbackCaseLinks() {
        return List.of("1504259907353545", "1504259907353552");
    }

    private List<String> createPostCallbackCaseLinks() {
        return List.of("1504259907353552", "1504259907353594");
    }
}
