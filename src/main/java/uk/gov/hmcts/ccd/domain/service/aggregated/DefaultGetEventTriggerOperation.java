package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEventBuilder;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
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
    private final CaseUpdateViewEventBuilder caseUpdateViewEventBuilder;

    @Autowired
    public DefaultGetEventTriggerOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                           final UIDService uidService,
                                           @Qualifier("authorised") final StartEventOperation startEventOperation,
                                           @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                           final CaseUpdateViewEventBuilder caseUpdateViewEventBuilder) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
        this.startEventOperation = startEventOperation;
        this.draftGateway = draftGateway;
        this.caseUpdateViewEventBuilder = caseUpdateViewEventBuilder;
    }

    @Override
    public CaseUpdateViewEvent executeForCaseType(String caseTypeId, String eventId, Boolean ignoreWarning) {
        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                          eventId,
                                                                                          ignoreWarning);
        return caseUpdateViewEventBuilder.build(startEventTrigger,
                                             caseTypeId,
                                             eventId,
                                             null);
    }

    @Override
    public CaseUpdateViewEvent executeForCase(String caseReference,
                                              String eventId,
                                              Boolean ignoreWarning) {
        final CaseDetails caseDetails = getCaseDetails(caseReference);

        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForCase(caseReference,
                                                                                      eventId,
                                                                                      ignoreWarning);
        return caseUpdateViewEventBuilder.build(startEventTrigger,
                                             caseDetails.getCaseTypeId(),
                                             eventId,
                                             caseReference);
    }

    @Override
    public CaseUpdateViewEvent executeForDraft(String draftReference,
                                               Boolean ignoreWarning) {
        final DraftResponse draftResponse = draftGateway.get(Draft.stripId(draftReference));
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));

        String eventId = draftResponse.getDocument().getEventId();
        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForDraft(draftReference,
                                                                                       ignoreWarning);
        return caseUpdateViewEventBuilder.build(startEventTrigger,
                                             caseDetails.getCaseTypeId(),
                                             eventId,
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
