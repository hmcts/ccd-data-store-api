package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

public interface CommonField {

    FieldTypeDefinition getFieldTypeDefinition();

    String getId();

    List<AccessControlList> getAccessControlLists();

    String getDisplayContext();

    void setDisplayContext(String displayContext);

    String getDisplayContextParameter();

    void setDisplayContextParameter(String displayContextParameter);

    Object getFormattedValue();

    void setFormattedValue(Object formattedValue);

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

    default DisplayContext displayContextType() {
        return Optional.ofNullable(getDisplayContext())
            .filter(dc -> !dc.equals("HIDDEN"))
            .map(DisplayContext::valueOf)
            .orElse(null);
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
        return this.getFieldTypeDefinition().getNestedField(path, false);
    }

}
