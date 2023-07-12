package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@ApiModel
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class CaseFieldDefinition implements Serializable, CommonField {

    private static final long serialVersionUID = -4257574164546267919L;

    @NonFinal
    @Setter
    String id;
    @JsonProperty("case_type_id")
    @Setter
    @NonFinal
    String caseTypeId;
    String label;
    @JsonProperty("hint_text")
    String hintText;
    @JsonProperty("field_type")
    FieldTypeDefinition fieldTypeDefinition;
    Boolean hidden;
    @JsonProperty("security_classification")
    String securityLabel;
    @JsonProperty("live_from")
    String liveFrom;
    @JsonProperty("live_until")
    String liveUntil;
    Integer order;
    @JsonProperty("show_condition")
    @NonFinal
    String showCondition;
    @JsonProperty("acls")
    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    @NonFinal
    @Builder.Default
    private List<AccessControlList> accessControlLists = new ArrayList<>();
    @JsonProperty("complexACLs")
    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    @Builder.Default
    private List<ComplexACL> complexACLs = new ArrayList<>();
    boolean metadata;
    @JsonProperty("display_context")
    @NonFinal
    String displayContext;
    @JsonProperty("display_context_parameter")
    @NonFinal
    String displayContextParameter;
    @JsonProperty("retain_hidden_value")
    @NonFinal
    Boolean retainHiddenValue;
    @JsonProperty("formatted_value")
    @NonFinal
    Object formattedValue;
    @JsonProperty("category_id")
    String categoryId;

    @Override
    public String getId() {
        return id;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getLabel() {
        return label;
    }

    public String getHintText() {
        return hintText;
    }

    @Override
    public FieldTypeDefinition getFieldTypeDefinition() {
        return fieldTypeDefinition;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public String getSecurityLabel() {
        return securityLabel;
    }

    public String getLiveFrom() {
        return liveFrom;
    }

    public String getLiveUntil() {
        return liveUntil;
    }

    public Integer getOrder() {
        return order;
    }

    public String getShowCondition() {
        return showCondition;
    }

    @Override
    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @Override
    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
    }

    public List<ComplexACL> getComplexACLs() {
        return complexACLs;
    }

    public boolean isMetadata() {
        return metadata;
    }


    @Override
    public String getDisplayContext() {
        return displayContext;
    }

    @Override
    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    @Override
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    @Override
    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    @Override
    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

    @Override
    public Object getFormattedValue() {
        return formattedValue;
    }

    @Override
    public void setFormattedValue(Object formattedValue) {
        this.formattedValue = formattedValue;
    }

    public String getCategoryId() {
        return categoryId;
    }

    @JsonIgnore
    public void propagateACLsToNestedFields() {
        propagateACLsToNestedFields(this, this.accessControlLists);
        applyComplexACLs();
        clearACLsForMissingComplexACLs();
    }

    private static void propagateACLsToNestedFields(CommonField caseField, List<AccessControlList> acls) {
        if (caseField.isCompoundFieldType()) {
            caseField.getFieldTypeDefinition().getChildren().forEach(nestedField -> {
                final List<AccessControlList> cloneACLs =
                    acls.stream().map(AccessControlList::duplicate).collect(toList());
                nestedField.setAccessControlLists(cloneACLs);
                propagateACLsToNestedFields(nestedField, acls);
            });
        }
    }

    private void applyComplexACLs() {
        this.complexACLs.forEach(complexACL -> {
            final CaseFieldDefinition nestedField =
                (CaseFieldDefinition) this.getComplexFieldNestedField(complexACL.getListElementCode())
                .orElseThrow(() -> new RuntimeException(
                    format("CaseField %s has no nested elements with code %s.",
                    this.getId(), complexACL.getListElementCode())));
            nestedField.getAccessControlListByRole(complexACL.getAccessProfile())
                .ifPresent(accessControlList -> nestedField.accessControlLists.remove(accessControlList));
            nestedField.getAccessControlLists().add(complexACL);

            propagateACLsToNestedFields(nestedField, nestedField.getAccessControlLists());
        });
    }

    private void clearACLsForMissingComplexACLs() {
        if (this.isCompoundFieldType()) {
            final List<String> allPaths =
                buildAllDottedComplexFieldPossibilities(this.getFieldTypeDefinition().getChildren());
            this.complexACLs.forEach(complexACL -> {
                Optional<String> parentPath = getParentPath(complexACL.getListElementCode());
                List<String> siblings;
                if (parentPath.isPresent()) {
                    siblings = filterSiblings(parentPath.get(), complexACL.getListElementCode(), allPaths);
                } else {
                    siblings = filterSiblings("", complexACL.getListElementCode(), allPaths);
                }
                removeACLS(findSiblingsWithNoComplexACLs(siblings), complexACL.getAccessProfile());
            });
        }
    }

    private void removeACLS(final List<String> siblingsWithNoComplexACLs, final String role) {
        siblingsWithNoComplexACLs.stream().forEach(s -> {
            final CaseFieldDefinition nestedElement = (CaseFieldDefinition) this.getComplexFieldNestedField(s)
                .orElseThrow(() -> new RuntimeException(
                    format("CaseField %s has no nested elements with code %s.", this.getId(), s)));
            nestedElement.getAccessControlListByRole(role)
                .ifPresent(acl -> nestedElement.getAccessControlLists().remove(acl));
            propagateACLsToNestedFields(nestedElement, nestedElement.getAccessControlLists());
        });
    }

    private List<String> findSiblingsWithNoComplexACLs(final List<String> siblings) {
        return siblings
            .stream()
            .filter(s -> this.complexACLs.stream()
                .noneMatch(complexACL -> complexACL.getListElementCode().equalsIgnoreCase(s)))
            .collect(toList());
    }

    private List<String> filterSiblings(String parent, String me, List<String> allPaths) {
        if (parent.equalsIgnoreCase("")) {
            return allPaths
                .stream()
                .filter(s -> s.indexOf('.') == -1
                    && !s.equalsIgnoreCase(me))
                .collect(toList());
        } else {
            return allPaths
                .stream()
                .filter(s -> s.startsWith(parent)
                    && !s.equalsIgnoreCase(parent)
                    && !s.equalsIgnoreCase(me)
                    && isNotAChild(parent, s))
                .collect(toList());
        }
    }

    private boolean isNotAChild(final String parent, final String s) {
        return s.indexOf('.', parent.length()) == parent.length()
            && (s.split("\\.").length == parent.split("\\.").length + 1);
    }

    private Optional<String> getParentPath(String path) {
        return path.lastIndexOf('.') > 0 ? Optional.of(path.substring(0, path.lastIndexOf('.'))) : Optional.empty();
    }

    @JsonIgnore
    public Optional<AccessControlList> getAccessControlListByRole(String role) {
        return this.accessControlLists.stream().filter(acl -> acl.getAccessProfile().equalsIgnoreCase(role))
            .findFirst();
    }

    private List<String> buildAllDottedComplexFieldPossibilities(List<CaseFieldDefinition> caseFieldEntities) {
        List<String> allSubTypePossibilities = new ArrayList<>();
        List<CaseFieldDefinition> fieldEntities = caseFieldEntities.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.<CaseFieldDefinition>toList());
        prepare(allSubTypePossibilities, "", fieldEntities);
        return allSubTypePossibilities;
    }

    private void prepare(List<String> allSubTypePossibilities,
                         String startingString,
                         List<CaseFieldDefinition> caseFieldDefinitions) {

        String concatenationCharacter = isBlank(startingString) ? "" : ".";
        caseFieldDefinitions.forEach(caseField -> {
            allSubTypePossibilities.add(startingString + concatenationCharacter + caseField.getId());

            List<CaseFieldDefinition> complexFields;
            if (caseField.getFieldTypeDefinition() == null) {
                complexFields = Collections.emptyList();
            } else if (isCollection(caseField)) {
                complexFields =
                    caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields();
            } else {
                complexFields = caseField.getFieldTypeDefinition().getComplexFields();
            }

            prepare(allSubTypePossibilities,
                startingString + concatenationCharacter + caseField.getId(),
                complexFields.stream().map(CaseFieldDefinition.class::cast).collect(toList()));
        });
    }

    private boolean isCollection(CommonField caseField) {
        return caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition() != null
            && caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields() != null
            && !caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields().isEmpty();
    }
}
