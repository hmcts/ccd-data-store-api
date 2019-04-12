package uk.gov.hmcts.ccd.domain.model.aggregated;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

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
        fillEmptyNestedFieldACLs(caseField, caseField.getAccessControlLists());

        return field;
    }

    public CaseViewField build(CaseField caseField, CaseEventField eventField, Object value) {
        final CaseViewField field = build(caseField, eventField);
        field.setValue(value);
        getFieldType(field, value);
        return field;
    }

    private void getFieldType(CaseViewField caseField, Object value) {

        if(caseField.getFieldType().getType().equals(DYNAMIC_LIST) && value != null) {
            caseField.setValue(((ObjectNode)value).get(DEFAULT));
            caseField.getFieldType().setDynamicListItems(processDynamicList((ObjectNode)value));
        }
    }

    private List<DynamicListItem> processDynamicList(ObjectNode value) {
        List<DynamicListItem> result = new ArrayList<>();
        value.get(DYNAMIC_LIST_ITEMS).elements().forEachRemaining(dynamicList -> {
            DynamicListItem dynamicListItem = new DynamicListItem();
            dynamicListItem.setCode(dynamicList.get(CODE).textValue());
            dynamicListItem.setLabel(dynamicList.get(LABEL).textValue());
            result.add(dynamicListItem);
        });
        return result;
    }

    public List<CaseViewField> build(List<CaseField> caseFields, List<CaseEventField> eventFields, Map<String, ?> data) {
        final Map<String, CaseField> caseFieldMap = caseFields.stream()
            .collect(Collectors.toMap(CaseField::getId, Function.identity()));

        return eventFields.stream()
            .filter(eventField -> caseFieldMap.containsKey(eventField.getCaseFieldId()))
            .map(eventField -> build(caseFieldMap.get(eventField.getCaseFieldId()), eventField, data != null ? data.get(eventField.getCaseFieldId()) : null))
            .collect(Collectors.toList());
    }

    private static void fillEmptyNestedFieldACLs(CaseField field, List<AccessControlList> acls) {
        if (field.getFieldType().getType().equalsIgnoreCase(COMPLEX) || field.getFieldType().getType().equalsIgnoreCase(COLLECTION)) {
            field.getFieldType().getComplexFields().forEach(nestedField -> {
                if (nestedField.getAccessControlLists() == null || nestedField.getAccessControlLists().isEmpty()) {
                    nestedField.setAccessControlLists(acls);
                }
                fillEmptyNestedFieldACLs(nestedField, acls);
            });
        }
    }
}
