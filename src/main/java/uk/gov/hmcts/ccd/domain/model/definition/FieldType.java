package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class FieldType implements Serializable {

    public static final String COLLECTION = "Collection";
    public static final String COMPLEX = "Complex";
    public static final String LABEL = "Label";
    public static final String CASE_PAYMENT_HISTORY_VIEWER = "CasePaymentHistoryViewer";
    public static final String CASE_HISTORY_VIEWER = "CaseHistoryViewer";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL = "AddressGlobal";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK = "AddressGlobalUK";
    public static final String PREDEFINED_COMPLEX_ADDRESS_UK = "AddressUK";
    public static final String PREDEFINED_COMPLEX_ORDER_SUMMARY = "OrderSummary";
    public static final String PREDEFINED_COMPLEX_CASELINK = "CaseLink";

    private String id = null;
    private String type = null;
    private BigDecimal min = null;
    private BigDecimal max = null;
    @JsonProperty("regular_expression")
    private String regularExpression = null;
    @JsonProperty("fixed_list_items")
    private List<FixedListItem> fixedListItems = new ArrayList<>();
    @JsonProperty("complex_fields")
    private List<CaseField> complexFields = new ArrayList<>();
    @JsonProperty("collection_field_type")
    private FieldType collectionFieldType = null;

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

    public List<FixedListItem> getFixedListItems() {
        return fixedListItems;
    }

    public void setFixedListItems(List<FixedListItem> fixedListItems) {
        this.fixedListItems = fixedListItems;
    }
    
    public List<CaseField> getComplexFields() {
        return complexFields;
    }

    public void setComplexFields(List<CaseField> complexFields) {
        this.complexFields = complexFields;
    }

    @JsonIgnore
    public List<CaseField> getChildren() {
        if (type.equalsIgnoreCase(COMPLEX)) {
            return complexFields;
        } else if (type.equalsIgnoreCase(COLLECTION)) {
            if (collectionFieldType == null) {
                return emptyList();
            }
            return collectionFieldType.complexFields;
        } else {
            return emptyList();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FieldType getCollectionFieldType() {
        return collectionFieldType;
    }

    public void setCollectionFieldType(FieldType collectionFieldType) {
        this.collectionFieldType = collectionFieldType;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
