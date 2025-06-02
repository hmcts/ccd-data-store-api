package uk.gov.hmcts.ccd.v2.external.controller;

import javax.inject.Inject;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.SubmitCaseTransaction;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

@Service
@Primary
@Profile("SECURITY_MOCK")
public class ContractTestSubmitCaseTransaction extends SubmitCaseTransaction {

    @Inject
    public ContractTestSubmitCaseTransaction(DefaultCaseDetailsRepository caseDetailsRepository,
                                             CaseAuditEventRepository caseAuditEventRepository,
                                             CaseTypeService caseTypeService,
                                             CallbackInvoker callbackInvoker,
                                             UIDService uidService,
                                             SecurityClassificationService securityClassificationService,
                                             CaseDataAccessControl caseDataAccessControl,
                                             MessageService messageService,
                                             CaseDocumentService caseDocumentService,
                                             ApplicationParams applicationParams,
                                             CaseAccessGroupUtils caseAccessGroupUtils,
                                             CaseDocumentTimestampService caseDocumentTimestampService) {
        super(caseDetailsRepository, caseAuditEventRepository, caseTypeService,
            callbackInvoker, uidService, securityClassificationService,
            caseDataAccessControl, messageService, caseDocumentService, applicationParams,
            caseAccessGroupUtils, caseDocumentTimestampService);

    }
}
