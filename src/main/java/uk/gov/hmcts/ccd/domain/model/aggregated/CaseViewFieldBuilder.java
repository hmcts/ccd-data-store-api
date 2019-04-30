package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.*;

@Named
@Singleton
public class CaseViewFieldBuilder {

    public static final String DYNAMIC_LIST_ITEMS = "dynamic_list_items";
    public static final String CODE = "code";
    public static final String DYNAMIC_LIST = "DynamicList";
    public static final String DEFAULT = "default";
    public static final String LABEL = "label";

    public CaseViewField build(CaseField caseField, CaseEventField eventField) {
        final CaseViewField field = new CaseViewField();

        field.setId(eventField.getCaseFieldId());
        field.setFieldType(caseField.getFieldType());
        field.setHidden(caseField.getHidden());
        field.setHintText(ofNullable(eventField.getHintText()).orElse(caseField.getHintText()));
        field.setLabel(ofNullable(eventField.getLabel()).orElse(caseField.getLabel()));
        field.setSecurityLabel(caseField.getSecurityLabel());
        field.setDisplayContext(eventField.getDisplayContext());
        field.setDisplayContextParameter(eventField.getDisplayContextParamter());
        field.setShowCondition(eventField.getShowCondition());
        field.setShowSummaryChangeOption(eventField.getShowSummaryChangeOption());
        field.setShowSummaryContentOption(eventField.getShowSummaryContentOption());
        field.setAccessControlLists(caseField.getAccessControlLists());

        caseField.propagateACLsToNestedFields();

        return field;
    }

    public CaseViewField build(CaseField caseField, CaseEventField eventField, Object value) {
        final CaseViewField field = build(caseField, eventField);
        field.setValue(value);
        getFieldTypeWithDynamicListsPopulated(field, value);
        return field;
    }

    public List<CaseViewField> build(List<CaseField> caseFields, List<CaseEventField> eventFields, Map<String, ?> data) {
        final Map<String, CaseField> caseFieldMap = caseFields.stream()
            .collect(Collectors.toMap(CaseField::getId, Function.identity()));

        return eventFields.stream()
            .filter(eventField -> caseFieldMap.containsKey(eventField.getCaseFieldId()))
            .map(eventField -> build(caseFieldMap.get(eventField.getCaseFieldId()), eventField, data != null ? data.get(eventField.getCaseFieldId()) : null))
            .collect(Collectors.toList());
    }

    private void getFieldTypeWithDynamicListsPopulated(CaseViewField caseField, Object value) {

        if (caseField.getFieldType().getType().equals(DYNAMIC_LIST) && value != null) {
            caseField.setValue(((ObjectNode) value).get(DEFAULT));
            caseField.getFieldType().setFixedListItems(processDynamicList((ObjectNode) value));
        }
    }

    private List<FixedListItem> processDynamicList(ObjectNode value) {
        List<FixedListItem> result = new ArrayList<>();
        value.get(DYNAMIC_LIST_ITEMS).elements().forEachRemaining(dynamicList -> {
            FixedListItem listItem = new FixedListItem();
            listItem.setCode(dynamicList.get(CODE).textValue());
            listItem.setLabel(dynamicList.get(LABEL).textValue());
            result.add(listItem);
        });
        return result;
    }
}
