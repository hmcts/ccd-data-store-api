package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

@Service
@Qualifier("default")
public class DefaultGetEventTriggerOperation implements GetEventTriggerOperation {

    private final CaseDetailsRepository caseDetailsRepository;
    private final StartEventOperation startEventOperation;
    private final UIDService uidService;
    private final DraftGateway draftGateway;
    private final CaseEventTriggerBuilder caseEventTriggerBuilder;

    @Autowired
    public DefaultGetEventTriggerOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                           final UIDService uidService,
                                           @Qualifier("authorised") final StartEventOperation startEventOperation,
                                           @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                           final CaseEventTriggerBuilder caseEventTriggerBuilder) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
        this.startEventOperation = startEventOperation;
        this.draftGateway = draftGateway;
        this.caseEventTriggerBuilder = caseEventTriggerBuilder;
    }

    @Override
    public CaseEventTrigger executeForCaseType(String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                          eventTriggerId,
                                                                                          ignoreWarning);
        return caseEventTriggerBuilder.build(startEventTrigger,
                                             caseTypeId,
                                             eventTriggerId,
                                             null);
    }

    @Override
    public CaseEventTrigger executeForCase(String caseReference,
                                           String eventTriggerId,
                                           Boolean ignoreWarning) {
        final CaseDetails caseDetails = getCaseDetails(caseReference);

        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForCase(caseReference,
                                                                                      eventTriggerId,
                                                                                      ignoreWarning);
        return caseEventTriggerBuilder.build(startEventTrigger,
                                             caseDetails.getCaseTypeId(),
                                             eventTriggerId,
                                             caseReference);
    }

    @Override
    public CaseEventTrigger executeForDraft(String draftReference,
                                            Boolean ignoreWarning) {
        final DraftResponse draftResponse = draftGateway.get(Draft.stripId(draftReference));
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));

        String eventTriggerId = draftResponse.getDocument().getEventTriggerId();
        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForDraft(draftReference,
                                                                                       ignoreWarning);
        return caseEventTriggerBuilder.build(startEventTrigger,
                                             caseDetails.getCaseTypeId(),
                                             eventTriggerId,
                                             draftReference);
    }

    private CaseDetails getCaseDetails(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return caseDetailsRepository.findByReference(caseReference).orElseThrow(
            () -> new CaseNotFoundException(caseReference));
    }

}
