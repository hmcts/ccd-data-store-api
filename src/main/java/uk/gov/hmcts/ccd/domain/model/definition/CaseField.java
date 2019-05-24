package uk.gov.hmcts.ccd.domain.model.definition;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@ToString
@ApiModel(description = "")
public class CaseField implements Serializable {

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
    @JsonProperty("show_condition")
    private String showConditon = null;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    @JsonProperty("complexACLs")
    private List<ComplexACL> complexACLs = new ArrayList<>();
    private boolean metadata;

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

    @JsonIgnore
    public boolean isCollectionFieldType() {
        return FieldType.COLLECTION.equalsIgnoreCase(fieldType.getType());
    }

    @JsonIgnore
    public void propagateACLsToNestedFields() {
        propagateACLsToNestedFields(this, this.accessControlLists);
        clearACLsForMissingComplexACLs();
        applyComplexACLs();
    }

    private void applyComplexACLs() {
        this.complexACLs.forEach(complexACL -> {
            final CaseField nestedField = this.findNestedElementByPath(complexACL.getListElementCode());
            nestedField.getAccessControlListByRole(complexACL.getRole())
                .ifPresent(accessControlList -> nestedField.accessControlLists.remove(accessControlList));
            nestedField.getAccessControlLists().add(complexACL);

            propagateACLsToNestedFields(nestedField, nestedField.getAccessControlLists());
        });
    }

    private void clearACLsForMissingComplexACLs() {
        if (isCompound(this)) {
            final List<String> allPaths = buildAllDottedComplexFieldPossibilities(this.getFieldType().getChildren());


            this.complexACLs.forEach(complexACL -> {
                Optional<String> parentPath = getParentPath(complexACL.getListElementCode());
                if (parentPath.isPresent()) {
                    final CaseField parent = this.findNestedElementByPath(parentPath.get());
                    if (isCompound(parent)) {
                        final List<CaseField> children = parent.getFieldType().getChildren();

                    }
                }
            });

            //findSiblingsWithMissingComplexACLs(this.findNestedElementByPath(parentPath), complexACL);

            // check siblings with no Complex ACLS
            // remove their ACLs for this role

        }
    }

    // for each complexACL
    // find parent path
    // the siblings are
    // all paths
    //    that start with parent
    //    that is not parent
    //    that is not me
    //    that doesn't have 2 "."s after parent (that are not my or my sibling's children)
    //
    // find siblings that don't have complexACLs added to root caseField
    // remove their ACLs which have role = complexACL.role


    private boolean isCompound(CaseField field) {
        return field.getFieldType().getType().equalsIgnoreCase(COMPLEX) || field.getFieldType().getType().equalsIgnoreCase(COLLECTION);
    }

    private boolean isMissingInComplexACLs(List<ComplexACL> complexACLs, String userRole, String parentCode) {
        return complexACLs.stream()
            .noneMatch(entity -> (entity.getRole() != null && entity.getRole().equalsIgnoreCase(userRole))
                && parentCode.equals(entity.getListElementCode())
            );
    }

    @JsonIgnore
    private Optional<String> getParentPath(String path) {
        return path.lastIndexOf('.') > 0 ? Optional.of(path.substring(0, path.lastIndexOf('.'))) : Optional.empty();
    }

    @JsonIgnore
    private Optional<AccessControlList> getAccessControlListByRole(String role) {
        return this.accessControlLists.stream().filter(acl -> acl.getRole().equalsIgnoreCase(role)).findFirst();
    }

    /**
     * Gets a caseField by specified path.
     *
     * @param path Path to a nested CaseField
     * @return A nested CaseField or 'this' when path is blank
     */
    @JsonIgnore
    public CaseField findNestedElementByPath(String path) {
        if (StringUtils.isBlank(path)) {
            return this;
        }
        if (this.getFieldType().getChildren().isEmpty()) {
            throw new BadRequestException(format("CaseField %s has no nested elements.", this.id));
        }
        List<String> pathElements = Arrays.stream(path.trim().split("\\.")).collect(Collectors.toList());

        return reduce(this.getFieldType().getChildren(), pathElements);
    }

    @JsonIgnore
    private CaseField reduce(List<CaseField> caseFields, List<String> pathElements) {
        String firstPathElement = pathElements.get(0);

        CaseField caseField = caseFields.stream().filter(e -> e.getId().equals(firstPathElement)).findFirst()
            .orElseThrow(() -> new BadRequestException(format("Nested element not found for %s", firstPathElement)));

        if (pathElements.size() == 1) {
            return caseField;
        } else {
            List<CaseField> newCaseFields = caseField.getFieldType().getChildren();
            List<String> tail = pathElements.subList(1, pathElements.size());

            return reduce(newCaseFields, tail);
        }
    }

    @JsonIgnore
    private static void propagateACLsToNestedFields(CaseField caseField, List<AccessControlList> acls) {
        if (caseField.getFieldType().getType().equalsIgnoreCase(COMPLEX) || caseField.getFieldType().getType().equalsIgnoreCase(COLLECTION)) {
            caseField.getFieldType().getChildren().forEach(nestedField -> {
//                if (nestedField.getAccessControlLists() == null || nestedField.getAccessControlLists().isEmpty()) {
                final List<AccessControlList> cloneACLs = acls.stream().map(accessControlList -> accessControlList.duplicate()).collect(Collectors.toList());
                nestedField.setAccessControlLists(cloneACLs);
//                }
                propagateACLsToNestedFields(nestedField, acls);
            });
        }
    }

    @JsonIgnore
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
                complexFields.stream().map(CaseField.class::cast).collect(Collectors.toList()));
        });
    }

    private boolean isCollection(CaseField caseField) {
        return caseField.getFieldType().getCollectionFieldType() != null
            && caseField.getFieldType().getCollectionFieldType().getComplexFields() != null
            && !caseField.getFieldType().getCollectionFieldType().getComplexFields().isEmpty();
    }
}
