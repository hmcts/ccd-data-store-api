package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.startevent.DefaultStartEventOperation;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.HashMap;


@Service
@Primary
@Qualifier("authorised")
@Profile("SECURITY_MOCK")
public class ContractTestStartEventOperation extends DefaultStartEventOperation {

    private HashMap<String, String> caseReferenceMap = new HashMap<>();
    private final ContractTestSecurityUtils contractTestSecurityUtils;

    public ContractTestStartEventOperation(EventTokenService eventTokenService,
                                           CaseDefinitionRepository caseDefinitionRepository,
                                           @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                               CaseDetailsRepository caseDetailsRepository,
                                           @Qualifier(DefaultDraftGateway.QUALIFIER) DraftGateway draftGateway,
                                           EventTriggerService eventTriggerService,
                                           CaseService caseService,
                                           UserAuthorisation userAuthorisation,
                                           CallbackInvoker callbackInvoker,
                                           UIDService uidService,
                                           ContractTestSecurityUtils contractTestSecurityUtils,
                                           CaseDataService caseDataService,
                                           TimeToLiveService timeToLiveService) {
        super(eventTokenService, caseDefinitionRepository, caseDetailsRepository, draftGateway,
            eventTriggerService, caseService, userAuthorisation,
            callbackInvoker, uidService, caseDataService, timeToLiveService);
        this.contractTestSecurityUtils = contractTestSecurityUtils;
    }


    @Override
    public StartEventResult triggerStartForCase(final String caseReference,
                                                final String eventId,
                                                final Boolean ignoreWarning) {


        String caseReferenceOverride = caseReferenceMap.get(eventId);

        contractTestSecurityUtils.setSecurityContextUserAsCaseworkerForEvent(eventId);
        return super.triggerStartForCase(caseReferenceOverride, eventId, ignoreWarning);
    }


    @Override
    public StartEventResult triggerStartForCaseType(final String caseTypeId,
                                                    final String eventId,
                                                    final Boolean ignoreWarning) {

        contractTestSecurityUtils.setSecurityContextUserAsCaseworkerForEvent(eventId);
        return super.triggerStartForCaseType(caseTypeId, eventId, ignoreWarning);
    }


    protected void setCaseReferenceOverride(String eventId, String caseReferenceOverride) {
        this.caseReferenceMap.put(eventId, caseReferenceOverride);
    }
}
