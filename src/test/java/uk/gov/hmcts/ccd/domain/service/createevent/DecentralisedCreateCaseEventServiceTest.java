package uk.gov.hmcts.ccd.domain.service.createevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;

@ExtendWith(MockitoExtension.class)
class DecentralisedCreateCaseEventServiceTest {

    @Mock
    private ServicePersistenceClient servicePersistenceClient;

    @InjectMocks
    private DecentralisedCreateCaseEventService service;

    @Captor
    private ArgumentCaptor<DecentralisedCaseEvent> caseEventCaptor;

    private DecentralisedCaseDetails decentralisedResponse;

    @BeforeEach
    void setUp() {
        decentralisedResponse = new DecentralisedCaseDetails();
        CaseDetails returnedDetails = new CaseDetails();
        returnedDetails.setId("2000");
        decentralisedResponse.setCaseDetails(returnedDetails);
        decentralisedResponse.setRevision(1L);

        when(servicePersistenceClient.createEvent(any(DecentralisedCaseEvent.class)))
            .thenReturn(decentralisedResponse);
    }

    @Test
    @DisplayName("Should include proxied user first and last name when present")
    void shouldPopulateProxiedUserNames() {
        Event event = new Event();
        event.setEventId("UpdateCase");
        event.setSummary("summary");
        event.setDescription("description");

        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setName("Update Case");

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId("CASE-TYPE");

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId("1001");
        caseDetails.setReference(1234567890123456L);
        caseDetails.setCaseTypeId("CASE-TYPE");
        caseDetails.setState("Open");

        IdamUser onBehalfOf = new IdamUser();
        onBehalfOf.setId("user-1");
        onBehalfOf.setForename("Alex");
        onBehalfOf.setSurname("Johnson");

        service.submitDecentralisedEvent(
            event,
            caseEventDefinition,
            caseTypeDefinition,
            caseDetails,
            Optional.empty(),
            Optional.of(onBehalfOf)
        );

        verify(servicePersistenceClient).createEvent(caseEventCaptor.capture());
        var captured = caseEventCaptor.getValue();

        assertThat(captured.getEventDetails().getProxiedByFirstName()).isEqualTo("Alex");
        assertThat(captured.getEventDetails().getProxiedByLastName()).isEqualTo("Johnson");
    }

    @Test
    @DisplayName("Should propagate resolved TTL to decentralised service and restore it on response")
    void shouldPropagateResolvedTtl() {
        Event event = new Event();
        event.setEventId("UpdateCase");

        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setName("Update Case");

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId("CASE-TYPE");

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId("1002");
        caseDetails.setReference(1234567890123457L);
        caseDetails.setCaseTypeId("CASE-TYPE");
        caseDetails.setState("Open");

        LocalDate resolvedTtl = LocalDate.now().plusDays(30);
        caseDetails.setResolvedTTL(resolvedTtl);
        decentralisedResponse.getCaseDetails().setResolvedTTL(null);

        DecentralisedCaseDetails result = service.submitDecentralisedEvent(
            event,
            caseEventDefinition,
            caseTypeDefinition,
            caseDetails,
            Optional.empty(),
            Optional.empty()
        );

        verify(servicePersistenceClient).createEvent(caseEventCaptor.capture());
        var captured = caseEventCaptor.getValue();

        assertThat(captured.getResolvedTtl()).isEqualTo(resolvedTtl);
        assertThat(result.getCaseDetails().getResolvedTTL()).isEqualTo(resolvedTtl);
    }
}
