package uk.gov.hmcts.ccd.test;

import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItemDefinition;

import java.math.BigDecimal;
import java.util.ArrayList;

import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FixedListItemBuilder.aFixedListItem;

public class CaseFieldDefinitionBuilder {

    private final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();

    public CaseFieldDefinitionBuilder(final String fieldId) {
        caseFieldDefinition.setId(fieldId);
        caseFieldDefinition.setFieldTypeDefinition(new FieldTypeDefinition());
    }

    public CaseFieldDefinitionBuilder withType(String type) {
        caseFieldDefinition.getFieldTypeDefinition().setType(type);
        return this;
    }

    public CaseFieldDefinitionBuilder withMax(Integer max) {
        return withMax(new BigDecimal(max));
    }

    public CaseFieldDefinitionBuilder withMax(Float max) {
        return withMax(new BigDecimal(max.toString()));
    }

    public CaseFieldDefinitionBuilder withMax(BigDecimal max) {
        caseFieldDefinition.getFieldTypeDefinition().setMax(max);
        return this;
    }

    public CaseFieldDefinitionBuilder withMin(Integer min) {
        return withMin(new BigDecimal(min));
    }

    public CaseFieldDefinitionBuilder withMin(Float min) {
        return withMin(new BigDecimal(min.toString()));
    }

    public CaseFieldDefinitionBuilder withMin(BigDecimal min) {
        caseFieldDefinition.getFieldTypeDefinition().setMin(min);
        return this;
    }

    public CaseFieldDefinitionBuilder withRegExp(String regExp) {
        caseFieldDefinition.getFieldTypeDefinition().setRegularExpression(regExp);
        return this;
    }

    public CaseFieldDefinition build() {
        return caseFieldDefinition;
    }

    public CaseFieldDefinitionBuilder withFixedListItem(String itemCode) {
        if (null == caseFieldDefinition.getFieldTypeDefinition().getFixedListItemDefinitions()) {
            caseFieldDefinition.getFieldTypeDefinition().setFixedListItemDefinitions(new ArrayList<>());
        }

        final FixedListItemDefinition item = aFixedListItem().build();
        item.setCode(itemCode);

        caseFieldDefinition.getFieldTypeDefinition()
            .getFixedListItemDefinitions()
            .add(item);
        return this;
    }

    public CaseFieldDefinitionBuilder withDynamicListItem(String itemCode, String itemValue) {
        if (null == caseFieldDefinition.getFieldTypeDefinition().getFixedListItemDefinitions()) {
            caseFieldDefinition.getFieldTypeDefinition().setFixedListItemDefinitions(new ArrayList<>());
        }

        final FixedListItemDefinition item = aFixedListItem().build();
        item.setCode(itemCode);
        item.setLabel(itemValue);

        caseFieldDefinition.getFieldTypeDefinition()
            .getFixedListItemDefinitions()
            .add(item);
        return this;
    }
}
