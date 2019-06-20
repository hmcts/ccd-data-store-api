package uk.gov.hmcts.ccd.domain.model.aggregated;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.Arrays;
import java.util.List;

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

    /**
     * Gets a caseField by specified path.
     *
     * @param path Path to a nested CaseField
     * @return A nested CaseField or 'this' when path is blank
     */
    @JsonIgnore
    default CommonField getComplexFieldNestedField(String path) {
        if (StringUtils.isBlank(path)) {
            return this;
        }
        if (this.getFieldType().getChildren().isEmpty()) {
            throw new BadRequestException(format("CaseField %s has no nested elements.", this.getId()));
        }
        List<String> pathElements = Arrays.stream(path.trim().split("\\.")).collect(toList());

        return reduce(this.getFieldType().getChildren(), pathElements);
    }

    @JsonIgnore
    default CaseField reduce(List<CaseField> caseFields, List<String> pathElements) {
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
}
