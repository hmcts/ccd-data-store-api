package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

public abstract class AbstractFieldProcessor {

    private final CaseViewFieldBuilder caseViewFieldBuilder;

    public AbstractFieldProcessor(CaseViewFieldBuilder caseViewFieldBuilder) {
        this.caseViewFieldBuilder = caseViewFieldBuilder;
    }

    public JsonNode execute(JsonNode node, CaseField caseField, CaseEventField caseEventField) {
        CaseViewField caseViewField = caseViewFieldBuilder.build(caseField, caseEventField);

        if (!BaseType.contains(caseViewField.getFieldType().getType())) {
            return node;
        }

        final BaseType fieldType = BaseType.get(caseViewField.getFieldType().getType());

        if (BaseType.get(COMPLEX) == fieldType) {
            return executeComplex(node, caseField.getFieldType().getComplexFields(), caseEventField);
        } else if (BaseType.get(COLLECTION) == fieldType) {
            return executeCollection(node, caseViewField);
        } else {
            return executeSimple(node, caseViewField, fieldType);
        }
    }

    protected abstract JsonNode executeSimple(JsonNode node, CaseViewField caseViewField, BaseType baseType);

    protected abstract JsonNode executeCollection(JsonNode collectionNode, CommonField caseViewField);

    protected abstract JsonNode executeComplex(JsonNode complexNode, List<CaseField> complexCaseFields, CaseEventField caseEventField);
}
