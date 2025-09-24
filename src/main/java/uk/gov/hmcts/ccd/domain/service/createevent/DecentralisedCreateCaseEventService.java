package uk.gov.hmcts.ccd.domain.service.createevent;

import java.util.Optional;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedEventDetails;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

@Slf4j
@RequiredArgsConstructor
@Service
public class DecentralisedCreateCaseEventService {

    private final ServicePersistenceClient servicePersistenceClient;


    public DecentralisedCaseDetails submitDecentralisedEvent(final Event event,
                                                             final CaseEventDefinition caseEventDefinition,
                                                             final CaseTypeDefinition caseTypeDefinition,
                                                             final CaseDetails caseDetails,
                                                             final Optional<CaseDetails> caseDetailsBefore,
                                                             final Optional<IdamUser> onBehalfOf) {
        DecentralisedEventDetails.DecentralisedEventDetailsBuilder eventDetails = DecentralisedEventDetails.builder()
                .caseType(caseTypeDefinition.getId())
                .eventId(event.getEventId())
                .eventName(caseEventDefinition.getName())
                .summary(event.getSummary())
                .description(event.getDescription());

        if (onBehalfOf.isPresent()) {
            var onBehalfOfUser = onBehalfOf.get();
            eventDetails.proxiedBy(onBehalfOfUser.getId())
                .proxiedByFirstName(onBehalfOfUser.getForename())
                .proxiedByLastName(onBehalfOfUser.getSurname());
        }

        DecentralisedCaseEvent decentralisedCaseEvent = DecentralisedCaseEvent.builder()
                .caseDetailsBefore(caseDetailsBefore.orElse(null))
                .caseDetails(caseDetails)
                .eventDetails(eventDetails.build())
                .resolvedTtl(caseDetails.getResolvedTTL())
                .internalCaseId(Long.valueOf(caseDetails.getId()))
                .build();

        try {
            var result = servicePersistenceClient.createEvent(decentralisedCaseEvent);
            // Resolved TTL has @JsonIgnore so restore it
            result.getCaseDetails().setResolvedTTL(caseDetails.getResolvedTTL());
            return result;
        } catch (FeignException.Conflict conflict) {
            throw new CaseConcurrencyException("""
                    Unfortunately we were unable to save your work to the case as \
                    another action happened at the same time.
                    Please review the case and try again.""");
        }
    }
}
