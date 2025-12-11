package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier(DefaultGetCaseHistoryViewOperation.QUALIFIER)
public class DefaultGetCaseHistoryViewOperation extends AbstractDefaultGetCaseViewOperation implements
    GetCaseHistoryViewOperation {

    public static final String QUALIFIER = "default";
    private static final String EVENT_NOT_FOUND = "Event history not found";

    private final GetEventsOperation getEventsOperation;
    private final FieldProcessorService fieldProcessorService;

    @Autowired
    public DefaultGetCaseHistoryViewOperation(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) GetCaseOperation getCaseOperation,
        @Qualifier("authorised") GetEventsOperation getEventsOperation,
        UIDefinitionRepository uiDefinitionRepository, CaseTypeService caseTypeService,
        UIDService uidService,
        DefaultObjectMapperService defaultObjectMapperService,
        CompoundFieldOrderService compoundFieldOrderService,
        FieldProcessorService fieldProcessorService) {

        super(getCaseOperation,
            uiDefinitionRepository,
            caseTypeService,
            uidService,
            defaultObjectMapperService,
            compoundFieldOrderService,
            fieldProcessorService);

        this.getEventsOperation = getEventsOperation;
        this.fieldProcessorService = fieldProcessorService;
    }

    @Override
    public CaseHistoryView execute(String caseReference, Long eventId) {
        validateCaseReference(caseReference);

        CaseDetails caseDetails = getCaseDetails(caseReference);
        String jurisdictionId = caseDetails.getJurisdiction();
        String caseTypeId = caseDetails.getCaseTypeId();
        CaseTypeDefinition caseTypeDefinition = getCaseType(jurisdictionId, caseTypeId);

        AuditEvent event = getEventsOperation.getEvent(caseDetails, caseTypeId, eventId).orElseThrow(
            () -> new ResourceNotFoundException(EVENT_NOT_FOUND));

        return merge(caseDetails, event, caseTypeDefinition);
    }

    private CaseHistoryView merge(CaseDetails caseDetails, AuditEvent event, CaseTypeDefinition caseTypeDefinition) {
        CaseHistoryView caseHistoryView = new CaseHistoryView();
        caseHistoryView.setCaseId(String.valueOf(caseDetails.getReference()));
        caseHistoryView.setCaseType(CaseViewType.createFrom(caseTypeDefinition));
        caseHistoryView.setTabs(getTabs(caseDetails, event.getData()));
        caseHistoryView.setEvent(CaseViewEvent.createFrom(event));

        return caseHistoryView;
    }

    /**
     * Override removes filterCaseTabFieldsBasedOnSecureData filter to allow data
     * fields missing from current case data to be returned in case history view.
     */
    @Override
    CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, ?> data) {
        CaseTypeTabsDefinition caseTypeTabsDefinition = getCaseTabCollection(caseDetails.getCaseTypeId());
        return caseTypeTabsDefinition.getTabs().stream().map(tab -> {
            CommonField[] caseViewFields = tab.getTabFields().stream()
                .map(caseTypeTabField -> CaseViewField.createFrom(caseTypeTabField, data))
                .map(fieldProcessorService::processCaseViewField)
                .toArray(CaseViewField[]::new);
            return new CaseViewTab(tab.getId(), tab.getLabel(), tab.getDisplayOrder(), (CaseViewField[])caseViewFields,
                                   tab.getShowCondition(), tab.getRole());

        }).toArray(CaseViewTab[]::new);
    }

}
