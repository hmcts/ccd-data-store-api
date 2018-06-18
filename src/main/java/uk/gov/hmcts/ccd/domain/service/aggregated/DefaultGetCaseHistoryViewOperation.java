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
import uk.gov.hmcts.ccd.domain.service.listevents.ListEventsOperation;

@Service
@Qualifier(DefaultGetCaseHistoryViewOperation.QUALIFIER)
public class DefaultGetCaseHistoryViewOperation extends AbstractDefaultGetCaseViewOperation implements GetCaseHistoryViewOperation {

    public static final String QUALIFIER = "default";

    private final ListEventsOperation listEventsOperation;

    @Autowired
    public DefaultGetCaseHistoryViewOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER) GetCaseOperation getCaseOperation,
                                              @Qualifier("authorised") ListEventsOperation listEventsOperation,
                                              UIDefinitionRepository uiDefinitionRepository, CaseTypeService caseTypeService,
                                              UIDService uidService) {
        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService);
        this.listEventsOperation = listEventsOperation;
    }

    @Override
    public CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId) {
        validateCaseReference(caseReference);

        CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        CaseDetails caseDetails = getCaseDetails(jurisdictionId, caseTypeId, caseReference);

        AuditEvent event = listEventsOperation.execute(jurisdictionId, caseTypeId, eventId);

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
