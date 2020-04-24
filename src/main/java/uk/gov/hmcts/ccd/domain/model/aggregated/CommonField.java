package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

public interface CommonField {

    FieldTypeDefinition getFieldTypeDefinition();

    String getId();

    List<AccessControlList> getAccessControlLists();

    String getDisplayContext();

    void setDisplayContext(String displayContext);

    String getDisplayContextParameter();

    void setDisplayContextParameter(String displayContext);

    @JsonIgnore
    default boolean isCollectionFieldType() {
        return FieldTypeDefinition.COLLECTION.equalsIgnoreCase(getFieldTypeDefinition().getType());
    }

    @JsonIgnore
    default boolean isComplexFieldType() {
        return COMPLEX.equalsIgnoreCase(getFieldTypeDefinition().getType());
    }

    @JsonIgnore
    default boolean isCompoundFieldType() {
        return isCollectionFieldType() || isComplexFieldType();
    }

    /**
     * Gets a caseField by specified path.
     *
     * @param path Path to a nested CaseField
     * @return A nested CaseField or 'this' when path is blank
     */
    @JsonIgnore
    default Optional<CommonField> getComplexFieldNestedField(String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.of(this);
        }
        if (this.getFieldTypeDefinition().getChildren().isEmpty()) {
            return Optional.empty();
        }
        List<String> pathElements = Arrays.stream(path.trim().split("\\.")).collect(toList());

        return reduce(this.getFieldTypeDefinition().getChildren(), pathElements);
    }

    @JsonIgnore
    default Optional<CommonField> reduce(List<CaseFieldDefinition> caseFieldDefinitions, List<String> pathElements) {
        String firstPathElement = pathElements.get(0);

        Optional<CaseFieldDefinition> optionalCaseField = caseFieldDefinitions.stream().filter(e -> e.getId().equals(firstPathElement)).findFirst();
        if (optionalCaseField.isPresent()) {
            CommonField caseField = optionalCaseField.get();

            if (pathElements.size() == 1) {
                return Optional.of(caseField);
            } else {
                List<CaseFieldDefinition> newCaseFieldDefinitions = caseField.getFieldTypeDefinition().getChildren();
                List<String> tail = pathElements.subList(1, pathElements.size());

                return reduce(newCaseFieldDefinitions, tail);
            }
        } else {
            return Optional.empty();
        }
    }

}
