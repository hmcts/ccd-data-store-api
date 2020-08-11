package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;

import static java.util.Collections.emptyList;

public class FieldTypeDefinition implements Serializable {

    public static final String COLLECTION = "Collection";
    public static final String COMPLEX = "Complex";
    public static final String MULTI_SELECT_LIST = "MultiSelectList";
    public static final String FIXED_LIST = "FixedList";
    public static final String FIXED_RADIO_LIST = "FixedRadioList";
    public static final String LABEL = "Label";
    public static final String CASE_PAYMENT_HISTORY_VIEWER = "CasePaymentHistoryViewer";
    public static final String CASE_HISTORY_VIEWER = "CaseHistoryViewer";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL = "AddressGlobal";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK = "AddressGlobalUK";
    public static final String PREDEFINED_COMPLEX_ADDRESS_UK = "AddressUK";
    public static final String PREDEFINED_COMPLEX_ORDER_SUMMARY = "OrderSummary";
    public static final String PREDEFINED_COMPLEX_CASELINK = "CaseLink";
    public static final String DATETIME = "DateTime";
    public static final String DATE = "Date";

    private String id = null;
    private String type = null;
    private BigDecimal min = null;
    private BigDecimal max = null;
    @JsonProperty("regular_expression")
    private String regularExpression = null;
    @JsonProperty("fixed_list_items")
    private List<FixedListItemDefinition> fixedListItemDefinitions = new ArrayList<>();
    @JsonProperty("complex_fields")
    private List<CaseFieldDefinition> complexFields = new ArrayList<>();
    @JsonProperty("collection_field_type")
    private FieldTypeDefinition collectionFieldTypeDefinition = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public void setRegularExpression(String regularExpression) {
        this.regularExpression = regularExpression;
    }

    public List<FixedListItemDefinition> getFixedListItemDefinitions() {
        return fixedListItemDefinitions;
    }

    public void setFixedListItemDefinitions(List<FixedListItemDefinition> fixedListItemDefinitions) {
        this.fixedListItemDefinitions = fixedListItemDefinitions;
    }

    public List<CaseFieldDefinition> getComplexFields() {
        return complexFields;
    }

    public void setComplexFields(List<CaseFieldDefinition> complexFields) {
        this.complexFields = complexFields;
    }

    @JsonIgnore
    public List<CaseFieldDefinition> getChildren() {
        if (isComplexFieldType()) {
            return complexFields;
        } else if (isCollectionFieldType()) {
            if (collectionFieldTypeDefinition == null) {
                return emptyList();
            }
            return collectionFieldTypeDefinition.complexFields;
        } else {
            return emptyList();
        }
    }

    @JsonIgnore
    public void setChildren(List<CaseFieldDefinition> caseFieldDefinitions) {
        if (type.equalsIgnoreCase(COMPLEX)) {
            complexFields = caseFieldDefinitions;
        } else if (type.equalsIgnoreCase(COLLECTION) && collectionFieldTypeDefinition != null) {
            collectionFieldTypeDefinition.complexFields = caseFieldDefinitions;
        }
    }

    @JsonIgnore
    public boolean isCollectionFieldType() {
        return type.equalsIgnoreCase(COLLECTION);
    }

    @JsonIgnore
    public boolean isComplexFieldType() {
        return type.equalsIgnoreCase(COMPLEX);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FieldTypeDefinition getCollectionFieldTypeDefinition() {
        return collectionFieldTypeDefinition;
    }

    public void setCollectionFieldTypeDefinition(FieldTypeDefinition collectionFieldTypeDefinition) {
        this.collectionFieldTypeDefinition = collectionFieldTypeDefinition;
    }

    public Optional<CommonField> getNestedField(String path, boolean pathIncludesParent) {
        return CaseFieldPathUtils.getFieldDefinitionByPath(this, path, pathIncludesParent);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
