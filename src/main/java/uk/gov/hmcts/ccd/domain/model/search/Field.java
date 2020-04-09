package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Field {
    private String id;
    private String elementPath;
    @JsonProperty("field_type")
    private FieldType type;
    private boolean metadata;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getElementPath() {
        return elementPath;
    }

    public void setElementPath(String elementPath) {
        this.elementPath = elementPath;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    public Optional<CommonField> getNestedField(String path) {
        final List<String> pathElements = Arrays.asList(path.trim().split("\\."));
        if (pathElements.size() == 1) {
            return Optional.empty();
        }

        return getType().getNestedField(pathElements.stream().skip(1).collect(Collectors.joining(".")));
    }
}
