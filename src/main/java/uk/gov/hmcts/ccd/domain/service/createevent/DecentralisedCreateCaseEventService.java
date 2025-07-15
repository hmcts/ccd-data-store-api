package uk.gov.hmcts.ccd.domain.service.createevent;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceAPI;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedEventDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

@Slf4j
@Service
public class DecentralisedCreateCaseEventService {

    private final CaseTypeService caseTypeService;
    private final ServicePersistenceAPI servicePersistenceAPI;
    private final PersistenceStrategyResolver resolver;

    public DecentralisedCreateCaseEventService(final CaseTypeService caseTypeService,
                                               final ServicePersistenceAPI servicePersistenceAPI,
                                               final PersistenceStrategyResolver resolver) {
        this.caseTypeService = caseTypeService;
        this.servicePersistenceAPI = servicePersistenceAPI;
        this.resolver = resolver;
    }

    public CaseDetails saveAuditEventForCaseDetails(final Event event,
                                                    final CaseEventDefinition caseEventDefinition,
                                                    final CaseDetails caseDetails,
                                                    final CaseTypeDefinition caseTypeDefinition,
                                                    final CaseDetails caseDetailsBefore
    ) {

        CaseStateDefinition caseStateDefinition =
                caseTypeService.findState(caseTypeDefinition, caseDetails.getState());

        DecentralisedEventDetails.DecentralisedEventDetailsBuilder eventDetails = DecentralisedEventDetails.builder()
                .caseType(caseTypeDefinition.getId())
                .eventId(event.getEventId())
                .eventName(caseEventDefinition.getName())
                .summary(event.getSummary())
                .description(event.getDescription())
                .stateName(caseStateDefinition.getName());

        //TODO Significant item is not yet set
        //auditEvent.setSignificantItem(aboutToSubmitCallbackResponse.getSignificantItem());

        DecentralisedCaseEvent decentralisedCaseEvent = DecentralisedCaseEvent.builder()
                .caseDetailsBefore(caseDetailsBefore)
                .caseDetails(caseDetails)
                .eventDetails(eventDetails.build())
                .build();

        try {
            var uri = resolver.resolveUriOrThrow(caseDetails);
            return servicePersistenceAPI.createEvent(uri, decentralisedCaseEvent);
        } catch (FeignException.Conflict conflict) {
            throw new CaseConcurrencyException("""
                    Unfortunately we were unable to save your work to the case as \
                    another action happened at the same time.
                    Please review the case and try again.""");

        }
    }
}
