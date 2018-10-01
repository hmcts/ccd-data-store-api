package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@Service
@Qualifier(DefaultGetCaseHistoryViewOperation.QUALIFIER)
public class DefaultGetCaseHistoryViewOperation extends AbstractDefaultGetCaseViewOperation implements
    GetCaseHistoryViewOperation {

    public static final String QUALIFIER = "default";
    private static final String EVENT_NOT_FOUND = "Event history not found";

    private final GetEventsOperation getEventsOperation;

    @Autowired
    public DefaultGetCaseHistoryViewOperation(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) GetCaseOperation getCaseOperation,
        @Qualifier("authorised") GetEventsOperation getEventsOperation,
        UIDefinitionRepository uiDefinitionRepository, CaseTypeService caseTypeService,
        UIDService uidService) {

        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService);
        this.getEventsOperation = getEventsOperation;
    }

    @Override
    public CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId) {
        validateCaseReference(caseReference);

        CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        CaseDetails caseDetails = getCaseDetails(caseReference);

        AuditEvent event = getEventsOperation.getEvent(jurisdictionId, caseTypeId, eventId).orElseThrow(
            () -> new ResourceNotFoundException(EVENT_NOT_FOUND));

        return merge(caseDetails, event, caseType);
    }

    private CaseHistoryView merge(CaseDetails caseDetails, AuditEvent event, CaseType caseType) {
        CaseHistoryView caseHistoryView = new CaseHistoryView();
        caseHistoryView.setCaseId(String.valueOf(caseDetails.getReference()));
        caseHistoryView.setCaseType(CaseViewType.createFrom(caseType));
        caseHistoryView.setTabs(getTabs(caseDetails, event.getData()));
        caseHistoryView.setEvent(CaseViewEvent.createFrom(event));

        return caseHistoryView;
    }

}
