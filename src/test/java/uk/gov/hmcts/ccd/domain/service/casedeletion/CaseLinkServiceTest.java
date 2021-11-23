package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;

import java.util.ArrayList;
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

    private static final String CASE_TYPE_ID = "Test";

    private static final long CASE_ID = 1L;

    @Test
    void updateCaseLinksDeletesAndInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(CASE_ID, "Test",
            createPreCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        Mockito.verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(CASE_ID, 1504259907353545L);
        Mockito.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                            1504259907353594L,
                                                                                            CASE_TYPE_ID);
    }

    @Test
    void updateCaseLinksDeletesCaseLinkFields() {
        caseLinkService.updateCaseLinks(CASE_ID, CASE_TYPE_ID,
            createPreCallbackCaseLinks(),
            EMPTY_LIST);

        Mockito.verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(CASE_ID, 1504259907353545L);
        Mockito.verify(caseLinkRepository).deleteByCaseReferenceAndLinkedCaseReference(CASE_ID, 1504259907353552L);
        Mockito.verify(caseLinkRepository, never()).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                                    1504259907353594L,
                                                                                                    CASE_TYPE_ID);

    }

    @Test
    void updateCaseLinksInsertsCaseLinkFields() {
        caseLinkService.updateCaseLinks(CASE_ID, CASE_TYPE_ID,
            EMPTY_LIST,
            createPostCallbackCaseLinks());

        Mockito.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                            1504259907353552L,
                                                                                            CASE_TYPE_ID);
        Mockito.verify(caseLinkRepository).insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(CASE_ID,
                                                                                            1504259907353594L,
                                                                                            CASE_TYPE_ID);
    }

    @Test
    void updateCaseLinksNoRepositoryInteractionCaseLinksIdentical() {
        caseLinkService.updateCaseLinks(CASE_ID,
            CASE_TYPE_ID,
            createPostCallbackCaseLinks(),
            createPostCallbackCaseLinks());

        Mockito.verify(caseLinkRepository, never())
            .insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(anyLong(), anyLong(), anyString());
        Mockito.verify(caseLinkRepository, never()).deleteByCaseReferenceAndLinkedCaseReference(anyLong(), anyLong());
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

        final List<String> caseLinks = new ArrayList<String>();
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
