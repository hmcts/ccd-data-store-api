package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.createcase.DefaultCreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createcase.SubmitCaseTransaction;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.domain.service.validate.CaseDataIssueLogger;
import uk.gov.hmcts.ccd.domain.service.validate.DefaultValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;

@Service
@Qualifier("authorised")
@Primary
@Profile("SECURITY_MOCK")
public class ContractTestCreateCaseOperation extends DefaultCreateCaseOperation {


    private String testCaseReference;
    private final ContractTestSecurityUtils contractTestSecurityUtils;


    public ContractTestCreateCaseOperation(@Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository,
                                           @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
                                               CaseDefinitionRepository caseDefinitionRepository,
                                           EventTriggerService eventTriggerService,
                                           EventTokenService eventTokenService,
                                           CaseDataService caseDataService,
                                           SubmitCaseTransaction submitCaseTransaction,
                                           CaseSanitiser caseSanitiser,
                                           CallbackInvoker callbackInvoker,
                                           @Qualifier(DefaultValidateCaseFieldsOperation.QUALIFIER)
                                           ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                           CasePostStateService casePostStateService,
                                           @Qualifier(DefaultDraftGateway.QUALIFIER) DraftGateway draftGateway,
                                           ContractTestSecurityUtils contractTestSecurityUtils,
                                           CaseDataIssueLogger caseDataIssueLogger,
                                           GlobalSearchProcessorService globalSearchProcessorService,
                                           @Qualifier("default")
                                               SupplementaryDataUpdateOperation supplementaryDataUpdateOperation,
                                           SupplementaryDataUpdateRequestValidator supplementaryDataValidator,
                                           CaseLinkService caseLinkService,
                                           TimeToLiveService timeToLiveService) {
        super(userRepository, caseDefinitionRepository, eventTriggerService, eventTokenService, caseDataService,
            submitCaseTransaction, caseSanitiser, callbackInvoker, validateCaseFieldsOperation,
            casePostStateService, draftGateway, caseDataIssueLogger, globalSearchProcessorService,
            supplementaryDataUpdateOperation, supplementaryDataValidator, caseLinkService, timeToLiveService);
        this.contractTestSecurityUtils = contractTestSecurityUtils;
    }

    @Override
    public CaseDetails createCaseDetails(final String caseTypeId,
                                         final CaseDataContent caseDataContent,
                                         final Boolean ignoreWarning) {
        contractTestSecurityUtils.setSecurityContextUserAsCaseworkerForCaseType(caseTypeId);
        return super.createCaseDetails(caseTypeId, caseDataContent, ignoreWarning);

    }
}
