package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class FieldTypeDefinition implements Serializable {

    public static final String COLLECTION = "Collection";
    public static final String COMPLEX = "Complex";
    public static final String MULTI_SELECT_LIST = "MultiSelectList";
    public static final String FIXED_LIST = "FixedList";
    public static final String DYNAMIC_FIXED_LIST = "DynamicFixedList";
    public static final String NUMBER = "Number";
    public static final String MONEY_GBP = "MoneyGBP";
    public static final String YES_OR_NO = "YesOrNo";
    public static final String FIXED_RADIO_LIST = "FixedRadioList";
    public static final String DYNAMIC_LIST = "DynamicList";
    public static final String DYNAMIC_RADIO_LIST = "DynamicRadioList";
    public static final String DYNAMIC_MULTI_SELECT_LIST = "DynamicMultiSelectList";
    public static final String LABEL = "Label";
    public static final String CASE_PAYMENT_HISTORY_VIEWER = "CasePaymentHistoryViewer";
    public static final String CASE_HISTORY_VIEWER = "CaseHistoryViewer";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL = "AddressGlobal";
    public static final String PREDEFINED_COMPLEX_ORGANISATION_POLICY = "OrganisationPolicy";
    public static final String PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST = "ChangeOrganisationRequest";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK = "AddressGlobalUK";
    public static final String PREDEFINED_COMPLEX_ADDRESS_UK = "AddressUK";
    public static final String PREDEFINED_COMPLEX_ORDER_SUMMARY = "OrderSummary";
    public static final String PREDEFINED_COMPLEX_CASELINK = "CaseLink";
    public static final String DATETIME = "DateTime";
    public static final String DATE = "Date";
    public static final String DOCUMENT = "Document";
    public static final String TEXT = "Text";
    public static final String WAYS_TO_PAY = "WaysToPay";
    public static final String FLAG_LAUNCHER = "FlagLauncher";
    public static final String COMPONENT_LAUNCHER = "ComponentLauncher";

    @NonFinal
    @Setter
    String id;
    String type;
    BigDecimal min;
    BigDecimal max;
    @JsonProperty("regular_expression")
    String regularExpression;
    @JsonProperty("fixed_list_items")
    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    @Builder.Default
    private List<FixedListItemDefinition> fixedListItemDefinitions = new ArrayList<>();
    @JsonProperty("complex_fields")
    @NonFinal
    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    @Setter
    @Builder.Default
    private List<CaseFieldDefinition> complexFields = new ArrayList<>();
    @JsonProperty("collection_field_type")
    @NonFinal
    @Setter
    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    private FieldTypeDefinition collectionFieldTypeDefinition;

    public String getType() {
        return type;
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public List<FixedListItemDefinition> getFixedListItemDefinitions() {
        return fixedListItemDefinitions;
    }

    public List<CaseFieldDefinition> getComplexFields() {
        return complexFields;
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

    @JsonIgnore
    public boolean isDynamicFieldType() {
        return type.equalsIgnoreCase(DYNAMIC_LIST)
            || type.equalsIgnoreCase(DYNAMIC_RADIO_LIST)
            || type.equalsIgnoreCase(DYNAMIC_MULTI_SELECT_LIST);
    }

    public String getId() {
        return id;
    }

    public FieldTypeDefinition getCollectionFieldTypeDefinition() {
        return collectionFieldTypeDefinition;
    }

    public Optional<CommonField> getNestedField(String path, boolean pathIncludesParent) {
        return CaseFieldPathUtils.getFieldDefinitionByPath(this, path, pathIncludesParent);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
