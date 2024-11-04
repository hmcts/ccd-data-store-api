package uk.gov.hmcts.ccd.domain.service.createevent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCCaseDetails;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCEventDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;

@Slf4j
@Service
public class POCCreateCaseEventService {

    private final CaseTypeService caseTypeService;
    private final PocApiClient pocApiClient;
    private final MessageService messageService;

    public POCCreateCaseEventService(final CaseTypeService caseTypeService,
                                     final PocApiClient pocApiClient,
                                     @Qualifier("caseEventMessageService") final MessageService messageService) {
        this.caseTypeService = caseTypeService;
        this.pocApiClient = pocApiClient;
        this.messageService = messageService;
    }

    public CaseDetails saveAuditEventForCaseDetails(final Event event,
                                                     final CaseEventDefinition caseEventDefinition,
                                                     final CaseDetails caseDetails,
                                                     final CaseTypeDefinition caseTypeDefinition,
                                                     final CaseDetails caseDetailsBefore
    ) {

        CaseStateDefinition caseStateDefinition =
                caseTypeService.findState(caseTypeDefinition, caseDetails.getState());

        POCEventDetails.POCEventDetailsBuilder eventDetails = POCEventDetails.builder()
                .eventId(event.getEventId())
                .eventName(caseEventDefinition.getName())
                .summary(event.getSummary())
                .description(event.getDescription())
                .stateName(caseStateDefinition.getName());

        //TODO Significant item is not yet set
        //auditEvent.setSignificantItem(aboutToSubmitCallbackResponse.getSignificantItem());

        POCCaseDetails pocCaseDetails = POCCaseDetails.builder()
                .caseDetailsBefore(caseDetailsBefore)
                .caseDetails(caseDetails)
                .eventDetails(eventDetails.build())
                .build();

        final CaseDetails savedPocCaseDetails = pocApiClient.createCase(pocCaseDetails);

        //TODO need to enable this feature
//        messageService.handleMessage(MessageContext.builder()
//                .caseDetails(caseDetails)
//                .caseTypeDefinition(caseTypeDefinition)
//                .caseEventDefinition(caseEventDefinition)
//                .oldState(caseDetailsBefore.getState())
//                .build());

        log.info("pocCaseDetails: {}", savedPocCaseDetails);
        return savedPocCaseDetails;
    }
}
