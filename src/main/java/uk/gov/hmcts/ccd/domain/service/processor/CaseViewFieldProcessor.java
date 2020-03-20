package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

public abstract class CaseViewFieldProcessor extends FieldProcessor {

    public CaseViewFieldProcessor(CaseViewFieldBuilder caseViewFieldBuilder) {
        super(caseViewFieldBuilder);
    }

    public CaseViewField execute(CaseViewField caseViewField) {
        final BaseType fieldType = BaseType.get(caseViewField.getFieldType().getType());

        if (BaseType.get(COMPLEX) == fieldType) {
            return executeComplex(caseViewField, caseViewField.getId(), caseViewField);
        } else if (BaseType.get(COLLECTION) == fieldType) {
            return executeCollection(caseViewField);
        } else {
            return executeSimple(caseViewField, fieldType);
        }
    }

    public CaseViewField execute(CaseViewField caseViewField, WizardPageField wizardPageField) {
        final BaseType fieldType = BaseType.get(caseViewField.getFieldType().getType());

        if (BaseType.get(COMPLEX) == fieldType) {
            return executeComplex(caseViewField, wizardPageField, caseViewField.getId(), caseViewField);
        } else if (BaseType.get(COLLECTION) == fieldType) {
            return executeCollection(caseViewField);
        } else {
            return executeSimple(caseViewField, fieldType);
        }
    }

    protected CaseViewField executeComplex(CaseViewField caseViewField, String fieldPrefix, CaseViewField topLevelField) {
        return executeComplex(caseViewField, null, fieldPrefix, topLevelField);
    }

    protected CaseViewField executeComplex(CaseViewField caseViewField, WizardPageField wizardPageField, String fieldPrefix, CaseViewField topLevelField) {
        caseViewField.setFormattedValue(
            caseViewField.getValue() instanceof ObjectNode ?
                executeComplex((ObjectNode) caseViewField.getValue(), caseViewField.getFieldType().getComplexFields(), wizardPageField, fieldPrefix, topLevelField) :
                caseViewField.getValue()
        );
        return caseViewField;
    }

    protected abstract CaseViewField executeSimple(CaseViewField caseViewField, BaseType baseType);

    protected abstract CaseViewField executeCollection(CaseViewField caseViewField);
}
