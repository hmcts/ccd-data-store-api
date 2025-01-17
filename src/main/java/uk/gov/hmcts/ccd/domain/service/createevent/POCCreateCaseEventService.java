package uk.gov.hmcts.ccd.domain.service.createevent;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCCaseEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.POCEventDetails;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

@Slf4j
@Service
public class POCCreateCaseEventService {

    private final CaseTypeService caseTypeService;
    private final PocApiClient pocApiClient;
    private final MessageService messageService;
    private final SecurityUtils securityUtils;
    private final RoleAssignmentService roleAssignmentService;

    public POCCreateCaseEventService(final CaseTypeService caseTypeService,
                                     final PocApiClient pocApiClient,
                                     @Qualifier("caseEventMessageService") final MessageService messageService,
                                     final SecurityUtils securityUtils,
                                     final RoleAssignmentService roleAssignmentService) {
        this.caseTypeService = caseTypeService;
        this.pocApiClient = pocApiClient;
        this.messageService = messageService;
        this.securityUtils = securityUtils;
        this.roleAssignmentService = roleAssignmentService;
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

        POCCaseEvent pocCaseEvent = POCCaseEvent.builder()
                .caseDetailsBefore(caseDetailsBefore)
                .caseDetails(caseDetails)
                .eventDetails(eventDetails.build())
                .build();

        try {
            RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
            return pocApiClient.createEvent(pocCaseEvent, roleAssignments);
        } catch (FeignException.Conflict conflict) {
            throw new CaseConcurrencyException("""
                    Unfortunately we were unable to save your work to the case as \
                    another action happened at the same time.
                    Please review the case and try again.""");

        }
    }
}
