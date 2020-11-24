package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.service.validate.DefaultValidateCaseFieldsOperation;

import javax.inject.Inject;

@Service
@Qualifier("contractTest")
public class ContractTestValidateCaseFieldsOperation extends DefaultValidateCaseFieldsOperation {

    @Inject
    ContractTestValidateCaseFieldsOperation(
        @Qualifier("contractTest") final CaseDefinitionRepository caseDefinitionRepository,
        final CaseTypeService caseTypeService,
        @Qualifier("contractTest") final FieldProcessorService fieldProcessorService
    ) {

        super(caseDefinitionRepository, caseTypeService, fieldProcessorService);

    }

}
