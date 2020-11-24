package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.createcase.DefaultCreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createcase.SubmitCaseTransaction;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;

import javax.inject.Inject;

@Service
@Qualifier("contractTest")
public class ContractTestCreateCaseOperation extends DefaultCreateCaseOperation {

    @Inject
    public ContractTestCreateCaseOperation(@Qualifier("contractTest") UserRepository userRepository,
                                           @Qualifier("contractTest") CaseDefinitionRepository caseDefinitionRepository,
                                           EventTriggerService eventTriggerService,
                                           EventTokenService eventTokenService,
                                           CaseDataService caseDataService,
                                           @Qualifier("contractTest") SubmitCaseTransaction submitCaseTransaction,
                                           CaseSanitiser caseSanitiser,
                                           CaseTypeService caseTypeService,
                                           CallbackInvoker callbackInvoker,
                                           @Qualifier("contractTest") ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                           @Qualifier(DefaultDraftGateway.QUALIFIER) DraftGateway draftGateway) {
        super(userRepository, caseDefinitionRepository, eventTriggerService, eventTokenService, caseDataService, submitCaseTransaction, caseSanitiser,
            caseTypeService, callbackInvoker, validateCaseFieldsOperation, draftGateway);
    }

    @Override
    protected CaseDetails getCaseDetails(String caseTypeId, CaseDataContent caseDataContent, CaseTypeDefinition caseTypeDefinition,
                                         CaseEventDefinition caseEventDefinition) {
        final CaseDetails newCaseDetails = new CaseDetails();

        newCaseDetails.setCaseTypeId(caseTypeId);
        newCaseDetails.setJurisdiction(caseTypeDefinition.getJurisdictionId());
        newCaseDetails.setState("AwaitingPayment");
        newCaseDetails.setSecurityClassification(caseTypeDefinition.getSecurityClassification());
        return newCaseDetails;
    }

}
