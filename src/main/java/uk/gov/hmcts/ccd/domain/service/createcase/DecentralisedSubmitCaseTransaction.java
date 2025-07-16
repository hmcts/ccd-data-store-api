package uk.gov.hmcts.ccd.domain.service.createcase;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedEventDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

@Slf4j
@Service
public class DecentralisedSubmitCaseTransaction {

    private final CaseTypeService caseTypeService;
    private final ServicePersistenceClient servicePersistenceClient;
    private final PersistenceStrategyResolver resolver;

    public DecentralisedSubmitCaseTransaction(final CaseTypeService caseTypeService,
                                              final ServicePersistenceClient servicePersistenceAPI,
                                              final PersistenceStrategyResolver resolver
                                              ) {
        this.caseTypeService = caseTypeService;
        this.servicePersistenceClient = servicePersistenceAPI;
        this.resolver = resolver;
    }

    public CaseDetails saveAuditEventForCaseDetails(AboutToSubmitCallbackResponse response,
                                                    Event event,
                                                    CaseTypeDefinition caseTypeDefinition,
                                                    IdamUser idamUser,
                                                    CaseEventDefinition caseEventDefinition,
                                                    CaseDetails newCaseDetails,
                                                    IdamUser onBehalfOfUser) {

        CaseStateDefinition caseStateDefinition =
                caseTypeService.findState(caseTypeDefinition, newCaseDetails.getState());

        DecentralisedEventDetails.DecentralisedEventDetailsBuilder eventDetails = DecentralisedEventDetails.builder()
                .caseType(caseTypeDefinition.getId())
                .eventId(event.getEventId())
                .eventName(caseEventDefinition.getName())
                .summary(event.getSummary())
                .description(event.getDescription())
                .stateName(caseStateDefinition.getName());

        if (onBehalfOfUser != null) {

            eventDetails.proxiedBy(onBehalfOfUser.getId())
                    .proxiedByFirstName(onBehalfOfUser.getForename())
                    .proxiedByFirstName(onBehalfOfUser.getSurname());
        }

        DecentralisedCaseEvent decentralisedCaseEvent = DecentralisedCaseEvent.builder()
                .caseDetails(newCaseDetails).eventDetails(eventDetails.build()).build();


        try {
            var uri = resolver.resolveUriOrThrow(decentralisedCaseEvent.getCaseDetails());
            CaseDetails caseDetails = servicePersistenceClient.createEvent(uri, decentralisedCaseEvent);
            // We currently need an ID for the case details to be set because it is written to ccd's db.
            // TODO: remove when enableCaseUsersDbSync is switched off and that functionality removed.
            caseDetails.setId(caseDetails.getReference().toString());

            return caseDetails;
        } catch (FeignException.Conflict conflict) {
            throw new CaseConcurrencyException("""
                    Unfortunately we were unable to save your work to the case as \
                    another action happened at the same time.
                    Please review the case and try again.""");

        }
    }
}
