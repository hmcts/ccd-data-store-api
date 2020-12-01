package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.createevent.AuthorisedCreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateCaseEventService;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.DefaultCreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

import javax.inject.Inject;

@Service
@Qualifier("authorised")
@Primary
public class ContractTestCreateEventOperation extends DefaultCreateEventOperation {


    private String testCaseReference;
    private ContractTestSecurityUtils contractTestSecurityUtils;

    public ContractTestCreateEventOperation(EventValidator eventValidator,
                                            CreateCaseEventService createEventService,
                                            CallbackInvoker callbackInvoker, ContractTestSecurityUtils contractTestSecurityUtils) {
        super(eventValidator, createEventService, callbackInvoker);
        this.contractTestSecurityUtils = contractTestSecurityUtils;
    }


    @Override
    public CaseDetails createCaseEvent(String caseReference,
                                       CaseDataContent content) {
        contractTestSecurityUtils.setSecurityContextUserAsCaseworkerForEvent(content.getEventId());
        return super.createCaseEvent(testCaseReference, content);
    }

    public void setTestCaseReference(String testCaseReference) {
        this.testCaseReference = testCaseReference;
    }
}
