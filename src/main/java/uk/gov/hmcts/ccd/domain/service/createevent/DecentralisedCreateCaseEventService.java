package uk.gov.hmcts.ccd.domain.service.createevent;

import javax.swing.text.html.Option;

import java.util.Optional;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.data.persistence.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.data.persistence.DecentralisedEventDetails;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

@Slf4j
@RequiredArgsConstructor
@Service
public class DecentralisedCreateCaseEventService {

    private final CaseTypeService caseTypeService;
    private final ServicePersistenceClient servicePersistenceClient;
    private final DefaultCaseDetailsRepository caseDetailsRepository;


    public CaseDetails submitDecentralisedEvent(final Event event,
                                                final CaseEventDefinition caseEventDefinition,
                                                final CaseTypeDefinition caseTypeDefinition,
                                                final CaseDetails caseDetails,
                                                final Optional<CaseDetails> caseDetailsBefore,
                                                final Optional<IdamUser> onBehalfOf
                                                ) {

        // A remaining mutable local column is resolvedTTL, which we continue to synchronise locally.
        caseDetailsRepository.updateResolvedTtl(caseDetails.getReference(), caseDetails.getResolvedTTL());

        CaseStateDefinition caseStateDefinition =
                caseTypeService.findState(caseTypeDefinition, caseDetails.getState());

        DecentralisedEventDetails.DecentralisedEventDetailsBuilder eventDetails = DecentralisedEventDetails.builder()
                .caseType(caseTypeDefinition.getId())
                .eventId(event.getEventId())
                .eventName(caseEventDefinition.getName())
                .summary(event.getSummary())
                .description(event.getDescription())
                .stateName(caseStateDefinition.getName());

        if (onBehalfOf.isPresent()) {
            var onBehalfOfUser = onBehalfOf.get();
            eventDetails.proxiedBy(onBehalfOfUser.getId())
                .proxiedByFirstName(onBehalfOfUser.getForename())
                .proxiedByFirstName(onBehalfOfUser.getSurname());
        }

        DecentralisedCaseEvent decentralisedCaseEvent = DecentralisedCaseEvent.builder()
                .caseDetailsBefore(caseDetailsBefore.orElse(null))
                .caseDetails(caseDetails)
                .eventDetails(eventDetails.build())
                .build();

        try {
            var result = servicePersistenceClient.createEvent(decentralisedCaseEvent);
            // We currently need an ID for the case details to be set because it is written to ccd's db.
            // TODO: remove when enableCaseUsersDbSync is switched off and that functionality removed.
            result.setId(caseDetails.getReference().toString());
            return result;

        } catch (FeignException.Conflict conflict) {
            throw new CaseConcurrencyException("""
                    Unfortunately we were unable to save your work to the case as \
                    another action happened at the same time.
                    Please review the case and try again.""");
        }
    }
}
