package uk.gov.hmcts.ccd.test;

import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;

import java.math.BigDecimal;
import java.util.ArrayList;

import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FixedListItemBuilder.aFixedListItem;

public class CaseFieldBuilder {

    private final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();

    public CaseFieldBuilder(final String fieldId) {
        caseFieldDefinition.setId(fieldId);
        caseFieldDefinition.setFieldType(new FieldType());
    }

    public CaseFieldBuilder withType(String type) {
        caseFieldDefinition.getFieldType().setType(type);
        return this;
    }

    public CaseFieldBuilder withMax(Integer max) {
        return withMax(new BigDecimal(max));
    }

    public CaseFieldBuilder withMax(Float max) {
        return withMax(new BigDecimal(max.toString()));
    }

    public CaseFieldBuilder withMax(BigDecimal max) {
        caseFieldDefinition.getFieldType().setMax(max);
        return this;
    }

    public CaseFieldBuilder withMin(Integer min) {
        return withMin(new BigDecimal(min));
    }

    public CaseFieldBuilder withMin(Float min) {
        return withMin(new BigDecimal(min.toString()));
    }

    public CaseFieldBuilder withMin(BigDecimal min) {
        caseFieldDefinition.getFieldType().setMin(min);
        return this;
    }

    public CaseFieldBuilder withRegExp(String regExp) {
        caseFieldDefinition.getFieldType().setRegularExpression(regExp);
        return this;
    }

    public CaseFieldDefinition build() {
        return caseFieldDefinition;
    }

    public CaseFieldBuilder withFixedListItem(String itemCode) {
        if (null == caseFieldDefinition.getFieldType().getFixedListItems()) {
            caseFieldDefinition.getFieldType().setFixedListItems(new ArrayList<>());
        }

        final FixedListItem item = aFixedListItem().build();
        item.setCode(itemCode);

        caseFieldDefinition.getFieldType()
            .getFixedListItems()
            .add(item);
        return this;
    }

    public CaseFieldBuilder withDynamicListItem(String itemCode, String itemValue) {
        if (null == caseFieldDefinition.getFieldType().getFixedListItems()) {
            caseFieldDefinition.getFieldType().setFixedListItems(new ArrayList<>());
        }

        final FixedListItem item = aFixedListItem().build();
        item.setCode(itemCode);
        item.setLabel(itemValue);

        caseFieldDefinition.getFieldType()
            .getFixedListItems()
            .add(item);
        return this;
    }
}
