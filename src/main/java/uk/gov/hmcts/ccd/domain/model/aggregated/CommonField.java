package uk.gov.hmcts.ccd.domain.model.aggregated;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.PREDEFINED_COMPLEX_ADDRESS_GLOBAL;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.PREDEFINED_COMPLEX_ADDRESS_UK;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.PREDEFINED_COMPLEX_CASELINK;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.PREDEFINED_COMPLEX_ORDER_SUMMARY;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

public interface CommonField {

    FieldType getFieldType();

    String getId();

    List<AccessControlList> getAccessControlLists();

    void setDisplayContext(String displayContext);

    @JsonIgnore
    default boolean isCollectionFieldType() {
        return FieldType.COLLECTION.equalsIgnoreCase(getFieldType().getType());
    }

    @JsonIgnore
    default boolean isComplexFieldType() {
        return COMPLEX.equalsIgnoreCase(getFieldType().getType());
    }

    @JsonIgnore
    default boolean isCompound() {
        return isCollectionFieldType() || isComplexFieldType();
    }

    @JsonIgnore
    default boolean isPredefinedComplexType() {
        return PREDEFINED_COMPLEX_ADDRESS_GLOBAL.equalsIgnoreCase(getFieldType().getId()) ||
            PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK.equalsIgnoreCase(getFieldType().getId()) ||
            PREDEFINED_COMPLEX_ADDRESS_UK.equalsIgnoreCase(getFieldType().getId()) ||
            PREDEFINED_COMPLEX_ORDER_SUMMARY.equalsIgnoreCase(getFieldType().getId()) ||
            PREDEFINED_COMPLEX_CASELINK.equalsIgnoreCase(getFieldType().getId());
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
        if (this.getFieldType().getChildren().isEmpty()) {
            return Optional.empty();
        }
        List<String> pathElements = Arrays.stream(path.trim().split("\\.")).collect(toList());

        return reduce(this.getFieldType().getChildren(), pathElements);
    }

    @JsonIgnore
    default Optional<CommonField> reduce(List<CaseField> caseFields, List<String> pathElements) {
        String firstPathElement = pathElements.get(0);

        Optional<CaseField> optionalCaseField = caseFields.stream().filter(e -> e.getId().equals(firstPathElement)).findFirst();
        if (optionalCaseField.isPresent()) {
            CaseField caseField = optionalCaseField.get();

            if (pathElements.size() == 1) {
                return Optional.of(caseField);
            } else {
                List<CaseField> newCaseFields = caseField.getFieldType().getChildren();
                List<String> tail = pathElements.subList(1, pathElements.size());

                return reduce(newCaseFields, tail);
            }
        } else {
            return Optional.empty();
        }
    }
}
