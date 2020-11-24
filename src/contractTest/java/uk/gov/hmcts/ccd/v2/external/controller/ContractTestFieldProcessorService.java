package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.processor.CaseDataFieldProcessor;
import uk.gov.hmcts.ccd.domain.service.processor.CaseViewFieldProcessor;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;

import java.util.List;
import javax.inject.Inject;

@Service
@Qualifier("contractTest")
public class ContractTestFieldProcessorService extends FieldProcessorService {

    @Autowired
    public ContractTestFieldProcessorService(
        List<CaseDataFieldProcessor> caseDataFieldProcessors,
        List<CaseViewFieldProcessor> caseViewFieldProcessors,
        @Qualifier("contractTest") UIDefinitionRepository uiDefinitionRepository,
        EventTriggerService eventTriggerService) {
        super(caseDataFieldProcessors, caseViewFieldProcessors, uiDefinitionRepository, eventTriggerService);
    }
}
