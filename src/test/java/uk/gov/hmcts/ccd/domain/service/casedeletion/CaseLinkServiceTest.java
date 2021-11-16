package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;

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
    private CaseLinkMapper caseLinkMapper;

    @InjectMocks
    private CaseLinkService caseLinkService;

    @Test
    void updateCaseLinksDeletesAndInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(1L, "Test",
            createPreCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(1L, 1504259907353545L);
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                            1504259907353594L,
                                                                                            "Test");
    }

    @Test
    void updateCaseLinksDeletesCaseLinkFields() {
        caseLinkService.updateCaseLinks(1L, "Test",
            createPreCallbackCaseLinks(),
            EMPTY_LIST);

        verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(1L, 1504259907353545L);
        verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(1L, 1504259907353552L);
        verify(caseLinkRepository, never()).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                                    1504259907353594L,
                                                                                                    "Test");

    }

    @Test
    void updateCaseLinksInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(1L, "Test",
            EMPTY_LIST,
            createPostCallbackCaseLinks());

        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                            1504259907353552L,
                                                                                            "Test");
        verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                            1504259907353594L,
                                                                                            "Test");
    }

    @Test
    void updateCaseLinksNoRepositoryInteractionCaseLinksIdentical() {
        caseLinkService.updateCaseLinks(1L,
            "Test",
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
        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(caseId, linkedCaseIds.get(0), caseTypeId),
            new CaseLinkEntity(caseId, linkedCaseIds.get(1), caseTypeId),
            new CaseLinkEntity(caseId, linkedCaseIds.get(2), caseTypeId));

        final List<CaseLink> caseLinksModels = List.of(
            new CaseLink(caseId, linkedCaseIds.get(0), caseTypeId),
            new CaseLink(caseId, linkedCaseIds.get(1), caseTypeId),
            new CaseLink(caseId, linkedCaseIds.get(2), caseTypeId)
        );

        when(caseLinkRepository.findAllByCaseReference(caseReferenceToFindLong)).thenReturn(caseLinkEntities);
        when(caseLinkMapper.entitiesToModels(caseLinkEntities)).thenReturn(caseLinksModels);

        final List<CaseLink> caseLinks = caseLinkService.findCaseLinks(caseReferenceToFind);
        assertFalse(caseLinks.isEmpty());

        final List<Long> foundLinkedCaseIds = caseLinks.stream()
            .map(CaseLink::getLinkedCaseId)
            .collect(Collectors.toList());

        assertTrue(foundLinkedCaseIds.containsAll(linkedCaseIds));
        verify(caseLinkRepository).findAllByCaseReference(caseReferenceToFindLong);
    }

    private List<String> createPreCallbackCaseLinks() {
        return List.of("1504259907353545", "1504259907353552");
    }

    private List<String> createPostCallbackCaseLinks() {
        return List.of("1504259907353552", "1504259907353594");
    }
}
