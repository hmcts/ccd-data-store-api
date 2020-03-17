package uk.gov.hmcts.ccd.domain.service.processor;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;

public abstract class CaseDataFieldProcessor extends FieldProcessor {

    public CaseDataFieldProcessor(CaseViewFieldBuilder caseViewFieldBuilder) {
        super(caseViewFieldBuilder);
    }
}
