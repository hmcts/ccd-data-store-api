package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinksResource;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {

    @Mock
    private CaseLinkService caseLinkService;

    @InjectMocks
    private TestingSupportController testingSupportController;

    @Test
    void getCaseLink_shouldCallFindCaseLinks() {

        // GIVEN
        String caseReference = "4444333322221111";
        List<CaseLink> expectedCaseLinks = List.of(new CaseLink());
        doReturn(expectedCaseLinks).when(caseLinkService).findCaseLinks(caseReference);

        // WHEN
        ResponseEntity<CaseLinksResource> actualResponse = testingSupportController.getCaseLink(caseReference);

        // THEN
        assertEquals(expectedCaseLinks, Objects.requireNonNull(actualResponse.getBody()).getCaseLinks());
        verify(caseLinkService, times(1)).findCaseLinks(caseReference);
    }

}
