package uk.gov.hmcts.ccd.domain.service.createcase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCCaseDetails;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCEventDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;

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

        POCCaseDetails pocCaseDetails = POCCaseDetails.builder()
                .caseDetails(newCaseDetails).eventDetails(eventDetails.build()).build();

        final CaseDetails savedPocCaseDetails = pocApiClient.createCase(pocCaseDetails);

        log.info("pocCaseDetails: {}", savedPocCaseDetails);
        log.info("pocCaseDetails id: {}", savedPocCaseDetails.getId());
        log.info("pocCaseDetails reference before: {}", savedPocCaseDetails.getReference());
        savedPocCaseDetails.setId(savedPocCaseDetails.getReference().toString());
        savedPocCaseDetails.setReference(newCaseDetails.getReference());
        log.info("pocCaseDetails reference: {}", savedPocCaseDetails.getReference());
        return savedPocCaseDetails;

    }
}
