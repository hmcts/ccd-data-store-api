package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;

import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CaseLinkServiceTest {

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @InjectMocks
    private CaseLinkService caseLinkService;

    @Test
    void updateCaseLinksDeletesAndInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(1L, "Test",
            createPreCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        Mockito.verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(1L, 1504259907353545L);
        Mockito.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                            1504259907353594L,
                                                                                            "Test");
    }

    @Test
    void updateCaseLinksDeletesCaseLinkFields() {
        caseLinkService.updateCaseLinks(1L, "Test",
            createPreCallbackCaseLinks(),
            EMPTY_LIST);

        Mockito.verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(1L, 1504259907353545L);
        Mockito.verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(1L, 1504259907353552L);
        Mockito.verify(caseLinkRepository, never()).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                                    1504259907353594L,
                                                                                                    "Test");

    }

    @Test
    void updateCaseLinksInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(1L, "Test",
            EMPTY_LIST,
            createPostCallbackCaseLinks());

        Mockito.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                            1504259907353552L,
                                                                                            "Test");
        Mockito.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1L,
                                                                                            1504259907353594L,
                                                                                            "Test");
    }

    @Test
    void updateCaseLinksNoRepositoryInteractionCaseLinksIdentical() {
        caseLinkService.updateCaseLinks(1L,
            "Test",
            createPostCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        Mockito.verify(caseLinkRepository, never())
            .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(anyLong(), anyLong(), anyString());
        Mockito.verify(caseLinkRepository, never()).deleteByCaseReferenceAndLinkedCaseReference(anyLong(), anyLong());
    }

    private List<String> createPreCallbackCaseLinks() {
        return List.of("1504259907353545", "1504259907353552");
    }

    private List<String> createPostCallbackCaseLinks() {
        return List.of("1504259907353552", "1504259907353594");
    }
}
