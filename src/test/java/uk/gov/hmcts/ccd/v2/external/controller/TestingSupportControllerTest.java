package uk.gov.hmcts.ccd.v2.external.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.BasicTypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
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
    @Mock
    private DateCaseClosedRepository dateCaseClosedRepository;

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
        when(nativeQuery.setParameterList(eq("caseTypeReferences"), anyList(), any(BasicTypeReference.class)))
            .thenReturn(nativeQuery);
        when(session.getTransaction())
            .thenReturn(transaction);
        testingSupportController.dataCaseTypeIdDelete(BigInteger.ONE, "Benefit");
        verify(session, times(4))
            .createNativeQuery(anyString());
    }

    @Test
    void shouldCreateDateCaseClosedRecord() {
        final LocalDateTime stateChangedDate = LocalDateTime.of(2025, 5, 8, 12, 30);
        TestingSupportController.DateCaseClosedRequest request =
            new TestingSupportController.DateCaseClosedRequest();
        request.setCcdCaseNumber(1234567890123456L);
        request.setState("Closed");
        request.setStateCategory("Closed");
        request.setStateChangedDate(stateChangedDate);
        when(dateCaseClosedRepository.save(any(DateCaseClosedEntity.class))).thenAnswer(invocation -> {
            DateCaseClosedEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        ResponseEntity<DateCaseClosedEntity> response = testingSupportController.dateCaseClosedPost(request);

        DateCaseClosedEntity responseBody = Objects.requireNonNull(response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1234567890123456L, responseBody.getCcdCaseNumber());
        assertEquals("Closed", responseBody.getState());
        assertEquals("Closed", responseBody.getStateCategory());
        assertEquals(stateChangedDate, responseBody.getStateChangedDate());
    }

    @Test
    void shouldDeleteExistingDateCaseClosedRecords() {
        when(sessionFactory.openSession())
            .thenReturn(session);
        when(session.createNativeQuery(anyString()))
            .thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any()))
            .thenReturn(nativeQuery);
        when(session.getTransaction())
            .thenReturn(transaction);

        ResponseEntity<Void> response = testingSupportController.dateCaseClosedDelete(LocalDate.of(2025, 1, 1));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(session).createNativeQuery(
            "DELETE FROM date_case_closed "
                + "WHERE state_changed_date < :stateChangedDateEnd"
        );
        verify(nativeQuery).setParameter("stateChangedDateEnd", Timestamp.valueOf("2025-01-02 00:00:00"));
    }
}
