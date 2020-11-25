package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.SubmitCaseTransaction;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import javax.inject.Inject;

@Service
@Primary
public class ContractTestSubmitCaseTransaction extends SubmitCaseTransaction {

    @Inject
    public ContractTestSubmitCaseTransaction(DefaultCaseDetailsRepository caseDetailsRepository,
                                             CaseAuditEventRepository caseAuditEventRepository,
                                             CaseTypeService caseTypeService,
                                             CallbackInvoker callbackInvoker,
                                             UIDService uidService,
                                             SecurityClassificationService securityClassificationService,
                                             DefaultCaseUserRepository caseUserRepository,
                                             UserAuthorisation userAuthorisation) {
        super(caseDetailsRepository, caseAuditEventRepository, caseTypeService, callbackInvoker, uidService, securityClassificationService,
            caseUserRepository, userAuthorisation);
    }
}
