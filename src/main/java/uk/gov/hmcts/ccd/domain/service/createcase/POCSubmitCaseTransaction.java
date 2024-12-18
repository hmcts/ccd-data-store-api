package uk.gov.hmcts.ccd.domain.service.createcase;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCCaseEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCEventDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

@Slf4j
@Service
public class POCSubmitCaseTransaction {

    private final CaseTypeService caseTypeService;
    private final PocApiClient pocApiClient;

    public POCSubmitCaseTransaction(final CaseTypeService caseTypeService, final PocApiClient pocApiClient) {
        this.caseTypeService = caseTypeService;
        this.pocApiClient = pocApiClient;
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

        POCEventDetails.POCEventDetailsBuilder eventDetails = POCEventDetails.builder()
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

        POCCaseEvent pocCaseEvent = POCCaseEvent.builder()
                .caseDetails(newCaseDetails).eventDetails(eventDetails.build()).build();


        try {
            CaseDetails caseDetails = pocApiClient.createEvent(pocCaseEvent);
            log.info("pocCaseDetails: {}", caseDetails);
            log.info("pocCaseDetails id: {}", caseDetails.getId());
            log.info("pocCaseDetails reference before: {}", caseDetails.getReference());
            caseDetails.setId(caseDetails.getReference().toString());
            caseDetails.setReference(caseDetails.getReference());
            log.info("pocCaseDetails reference: {}", caseDetails.getReference());

            return caseDetails;
        } catch (FeignException.Conflict conflict) {
            throw new CaseConcurrencyException("""
                    Unfortunately we were unable to save your work to the case as \
                    another action happened at the same time.
                    Please review the case and try again.""");

        }
    }
}
