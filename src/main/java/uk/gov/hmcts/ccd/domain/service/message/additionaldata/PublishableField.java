package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;

import static com.google.common.base.Strings.isNullOrEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishableField {

    private String key;
    private String path;
    private CommonField caseField;
    private DisplayContext displayContext;
    private boolean publishTopLevel;

    public PublishableField(CaseTypeDefinition caseTypeDefinition,
                            CaseEventFieldComplexDefinition caseEventFieldComplex,
                            String path) {
        setCommonFields(caseTypeDefinition, path,
            caseEventFieldComplex.getReference(), caseEventFieldComplex.getPublishAs());
        this.publishTopLevel = !isNullOrEmpty(caseEventFieldComplex.getPublishAs());
    }

    public PublishableField(CaseTypeDefinition caseTypeDefinition,
                            CaseEventFieldDefinition caseEventField) {
        setCommonFields(caseTypeDefinition, caseEventField.getCaseFieldId(),
            caseEventField.getCaseFieldId(), caseEventField.getPublishAs());
        this.publishTopLevel = true;
        this.displayContext = caseEventField.getDisplayContextEnum();
    }

    public boolean isNested() {
        return splitPath().length > 1;
    }

    public String getFieldId() {
        return StringUtils.substringAfterLast(path, ".");
    }

    private void setCommonFields(CaseTypeDefinition caseTypeDefinition,
                                 String path,
                                 String originalId,
                                 String publishAs) {
        this.path = path;
        this.key = getKey(publishAs, originalId);
        this.caseField = getCommonField(caseTypeDefinition, path);
    }

    private String getKey(String publishAs, String originalId) {
        return isNullOrEmpty(publishAs) ? originalId : publishAs;
    }

    private CommonField getCommonField(CaseTypeDefinition caseTypeDefinition, String path) {
        // TODO: Throw more useful exception!
        return caseTypeDefinition.getComplexSubfieldDefinitionByPath(path).orElseThrow();
    }

    private String[] splitPath() {
        return path.split("\\.");
    }
}
