package uk.gov.hmcts.ccd.domain.model.definition;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.ToString;

@ToString
@ApiModel(description = "")
public class CaseField implements Serializable, CommonField {

    private static final long serialVersionUID = -4257574164546267919L;

    private String id = null;
    @JsonProperty("case_type_id")
    private String caseTypeId = null;
    private String label = null;
    @JsonProperty("hint_text")
    private String hintText = null;
    @JsonProperty("field_type")
    private FieldType fieldType = null;
    private Boolean hidden = null;
    @JsonProperty("security_classification")
    private String securityLabel = null;
    @JsonProperty("live_from")
    private String liveFrom = null;
    @JsonProperty("live_until")
    private String liveUntil = null;
    private Integer order;
    @JsonProperty("show_condition")
    private String showConditon = null;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    @JsonProperty("complexACLs")
    private List<ComplexACL> complexACLs = new ArrayList<>();
    private boolean metadata;
    @JsonProperty("display_context")
    private String displayContext;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;
    @JsonProperty("formatted_value")
    private Object formattedValue;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getSecurityLabel() {
        return securityLabel;
    }

    public void setSecurityLabel(String securityLabel) {
        this.securityLabel = securityLabel;
    }

    public String getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(String liveFrom) {
        this.liveFrom = liveFrom;
    }

    public String getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(String liveUntil) {
        this.liveUntil = liveUntil;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(final Integer order) {
        this.order = order;
    }

    public String getShowConditon() {
        return showConditon;
    }

    public void setShowConditon(String showConditon) {
        this.showConditon = showConditon;
    }

    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
    }

    public List<ComplexACL> getComplexACLs() {
        return complexACLs;
    }

    public void setComplexACLs(List<ComplexACL> complexACLs) {
        this.complexACLs = complexACLs;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    public Object getFormattedValue() {
        return formattedValue;
    }

    public void setFormattedValue(Object formattedValue) {
        this.formattedValue = formattedValue;
    }

    @JsonIgnore
    public void propagateACLsToNestedFields() {
        propagateACLsToNestedFields(this, this.accessControlLists);
        applyComplexACLs();
        clearACLsForMissingComplexACLs();
    }

    private void applyComplexACLs() {
        this.complexACLs.forEach(complexACL -> {
            final CaseField nestedField = (CaseField) this.getComplexFieldNestedField(complexACL.getListElementCode())
                .orElseThrow(() -> new RuntimeException(format("CaseField %s has no nested elements with code %s.", this.getId(), complexACL.getListElementCode())));
            nestedField.getAccessControlListByRole(complexACL.getRole())
                .ifPresent(accessControlList -> nestedField.accessControlLists.remove(accessControlList));
            nestedField.getAccessControlLists().add(complexACL);

            propagateACLsToNestedFields(nestedField, nestedField.getAccessControlLists());
        });
    }

    private void clearACLsForMissingComplexACLs() {
        if (this.isCompoundFieldType()) {
            final List<String> allPaths = buildAllDottedComplexFieldPossibilities(this.getFieldType().getChildren());
            this.complexACLs.forEach(complexACL -> {
                Optional<String> parentPath = getParentPath(complexACL.getListElementCode());
                List<String> siblings;
                if (parentPath.isPresent()) {
                    siblings = filterSiblings(parentPath.get(), complexACL.getListElementCode(), allPaths);
                } else {
                    siblings = filterSiblings("", complexACL.getListElementCode(), allPaths);
                }
                removeACLS(findSiblingsWithNoComplexACLs(siblings), complexACL.getRole());
            });
        }
    }

    private void removeACLS(final List<String> siblingsWithNoComplexACLs, final String role) {
        siblingsWithNoComplexACLs.stream().forEach(s -> {
            final CaseField nestedElement = (CaseField) this.getComplexFieldNestedField(s)
                .orElseThrow(() -> new RuntimeException(format("CaseField %s has no nested elements with code %s.", this.getId(), s)));
            nestedElement.getAccessControlListByRole(role).ifPresent(acl -> nestedElement.getAccessControlLists().remove(acl));
            propagateACLsToNestedFields(nestedElement, nestedElement.getAccessControlLists());
        });
    }

    private List<String> findSiblingsWithNoComplexACLs(final List<String> siblings) {
        return siblings
            .stream()
            .filter(s -> this.complexACLs.stream().noneMatch(complexACL -> complexACL.getListElementCode().equalsIgnoreCase(s)))
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
    Optional<AccessControlList> getAccessControlListByRole(String role) {
        return this.accessControlLists.stream().filter(acl -> acl.getRole().equalsIgnoreCase(role)).findFirst();
    }

    private static void propagateACLsToNestedFields(CommonField caseField, List<AccessControlList> acls) {
        if (caseField.isCompoundFieldType()) {
            caseField.getFieldType().getChildren().forEach(nestedField -> {
                final List<AccessControlList> cloneACLs = acls.stream().map(AccessControlList::duplicate).collect(toList());
                nestedField.setAccessControlLists(cloneACLs);
                propagateACLsToNestedFields(nestedField, acls);
            });
        }
    }

    private List<String> buildAllDottedComplexFieldPossibilities(List<CaseField> caseFieldEntities) {
        List<String> allSubTypePossibilities = new ArrayList<>();
        List<CaseField> fieldEntities = caseFieldEntities.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.<CaseField>toList());
        prepare(allSubTypePossibilities, "", fieldEntities);
        return allSubTypePossibilities;
    }

    private void prepare(List<String> allSubTypePossibilities,
                         String startingString,
                         List<CaseField> caseFieldEntities) {

        String concatenationCharacter = isBlank(startingString) ? "" : ".";
        caseFieldEntities.forEach(caseField -> {
            allSubTypePossibilities.add(startingString + concatenationCharacter + caseField.getId());

            List<CaseField> complexFields;
            if (caseField.getFieldType() == null) {
                complexFields = Collections.emptyList();
            } else if (isCollection(caseField)) {
                complexFields = caseField.getFieldType().getCollectionFieldType().getComplexFields();
            } else {
                complexFields = caseField.getFieldType().getComplexFields();
            }

            prepare(allSubTypePossibilities,
                startingString + concatenationCharacter + caseField.getId(),
                complexFields.stream().map(CaseField.class::cast).collect(toList()));
        });
    }

    private boolean isCollection(CommonField caseField) {
        return caseField.getFieldType().getCollectionFieldType() != null
            && caseField.getFieldType().getCollectionFieldType().getComplexFields() != null
            && !caseField.getFieldType().getCollectionFieldType().getComplexFields().isEmpty();
    }
}
