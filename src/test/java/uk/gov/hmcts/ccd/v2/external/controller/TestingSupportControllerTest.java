package uk.gov.hmcts.ccd.v2.external.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinksResource;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Session session;
    @Mock
    private NativeQuery nativeQuery;
    @Mock
    private Transaction transaction;

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

    @Test
    void shouldDeleteCaseTypeIds() {
        when(sessionFactory.openSession())
            .thenReturn(session);
        when(session.createNativeQuery(anyString()))
            .thenReturn(nativeQuery);
        when(nativeQuery.setParameterList(eq("caseTypeReferences"), anyList(), isA(StringType.class)))
            .thenReturn(nativeQuery);
        when(session.getTransaction())
            .thenReturn(transaction);
        testingSupportController.dataCaseTypeIdDelete(BigInteger.ONE, "Benefit");
        verify(session, times(3))
            .createNativeQuery(anyString());
    }
}
