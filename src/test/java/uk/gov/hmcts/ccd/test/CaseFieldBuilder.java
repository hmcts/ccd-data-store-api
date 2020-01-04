package uk.gov.hmcts.ccd.test;

import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;

import java.math.BigDecimal;
import java.util.ArrayList;

import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FixedListItemBuilder.aFixedListItem;

public class CaseFieldBuilder {

    private final CaseField caseField = new CaseField();

    public CaseFieldBuilder(final String fieldId) {
        caseField.setId(fieldId);
        caseField.setFieldType(new FieldType());
    }

    public CaseFieldBuilder withType(String type) {
        caseField.getFieldType().setType(type);
        return this;
    }

    public CaseFieldBuilder withMax(Integer max) {
        return withMax(new BigDecimal(max));
    }

    public CaseFieldBuilder withMax(Float max) {
        return withMax(new BigDecimal(max.toString()));
    }

    public CaseFieldBuilder withMax(BigDecimal max) {
        caseField.getFieldType().setMax(max);
        return this;
    }

    public CaseFieldBuilder withMin(Integer min) {
        return withMin(new BigDecimal(min));
    }

    public CaseFieldBuilder withMin(Float min) {
        return withMin(new BigDecimal(min.toString()));
    }

    public CaseFieldBuilder withMin(BigDecimal min) {
        caseField.getFieldType().setMin(min);
        return this;
    }

    public CaseFieldBuilder withRegExp(String regExp) {
        caseField.getFieldType().setRegularExpression(regExp);
        return this;
    }

    public CaseField build() {
        return caseField;
    }

    public CaseFieldBuilder withFixedListItem(String itemCode) {
        if (null == caseField.getFieldType().getFixedListItems()) {
            caseField.getFieldType().setFixedListItems(new ArrayList<>());
        }

        final FixedListItem item = aFixedListItem().build();
        item.setCode(itemCode);

        caseField.getFieldType()
            .getFixedListItems()
            .add(item);
        return this;
    }

    public CaseFieldBuilder withDynamicListItem(String itemCode, String itemValue) {
        if (null == caseField.getFieldType().getFixedListItems()) {
            caseField.getFieldType().setFixedListItems(new ArrayList<>());
        }

        final FixedListItem item = aFixedListItem().build();
        item.setCode(itemCode);
        item.setLabel(itemValue);

        caseField.getFieldType()
            .getFixedListItems()
            .add(item);
        return this;
    }
}
