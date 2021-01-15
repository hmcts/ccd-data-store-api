package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishableField {

    public static final String FIELD_SEPARATOR = ".";

    private String key;
    private String path;
    private String value;
    private String originalId;
    private CommonField caseField;
    private DisplayContext displayContext;
    private boolean publishTopLevel;

    public PublishableField(CaseTypeDefinition caseTypeDefinition,
                            CaseEventFieldComplexDefinition caseEventFieldComplex,
                            String path,
                            CaseDetails caseDetails) {
        setCommonFields(caseTypeDefinition, path,
            caseEventFieldComplex.getReference(), caseEventFieldComplex.getPublishAs(), caseDetails);
        this.publishTopLevel = !isNullOrEmpty(caseEventFieldComplex.getPublishAs());
    }

    public PublishableField(CaseTypeDefinition caseTypeDefinition,
                            CaseEventFieldDefinition caseEventField,
                            CaseDetails caseDetails) {
        setCommonFields(caseTypeDefinition, caseEventField.getCaseFieldId(),
            caseEventField.getCaseFieldId(), caseEventField.getPublishAs(), caseDetails);
        this.publishTopLevel = true;
        this.displayContext = caseEventField.getDisplayContextEnum();
    }

    public FieldTypeDefinition getFieldType() {
        return caseField.getFieldTypeDefinition();
    }

    public boolean isNested() {
        return splitPath().length > 1;
    }

    public String getFieldId() {
        String fieldIdFromPath = StringUtils.substringAfterLast(path, FIELD_SEPARATOR);
        return isNullOrEmpty(fieldIdFromPath) ? caseField.getId() : fieldIdFromPath;
    }

    public String[] splitPath() {
        return path.split(Pattern.quote(FIELD_SEPARATOR));
    }

    public boolean isSubFieldOf(PublishableField publishableField) {
        return this.getPath().startsWith(publishableField.getPath() + FIELD_SEPARATOR);
    }

    public List<PublishableField> filterDirectChildrenFrom(List<PublishableField> publishableFields) {
        return publishableFields.stream()
            .filter(field -> field.isSubFieldOf(this) && pathSizeDifferenceTo(field) == 1)
            .collect(Collectors.toList());
    }

    private void setCommonFields(CaseTypeDefinition caseTypeDefinition,
                                 String path,
                                 String originalId,
                                 String publishAs,
                                 CaseDetails caseDetails) {
        this.path = path;
        this.key = getKey(publishAs, originalId);
        this.originalId = originalId;
        this.caseField = getCommonField(caseTypeDefinition, path);
        this.value = getValue(path, caseDetails);
    }

    private String getKey(String publishAs, String originalId) {
        return isNullOrEmpty(publishAs) ? originalId : publishAs;
    }

    private String getValue(String path, CaseDetails caseDetails) {
        if (!caseDetails.getData().keySet().contains(path)) {
            return null;
        } else {
            return caseDetails.getData().get(path).textValue();
        }
    }

    /**
     * Get the difference in path size (i.e. the difference in how nested two fields are).
     * @return If > 0, then the provided argument has MORE nested levels than this
     */
    private int pathSizeDifferenceTo(PublishableField publishableField) {
        return publishableField.splitPath().length - this.splitPath().length;
    }

    private CommonField getCommonField(CaseTypeDefinition caseTypeDefinition, String path) {
        return caseTypeDefinition.getComplexSubfieldDefinitionByPath(path)
            .orElseThrow(() -> new ServiceException(String.format("Case event field '%s' cannot be found "
                    + "in configuration for case type '%s'.", path, caseTypeDefinition.getId())));
    }
}
