package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;

@ConstructorBinding
@ConfigurationProperties("elasticsearch")
@AllArgsConstructor
@Getter
public class ElasticsearchMappings {

    private final TypeMappings typeMappings;
    private final CasePredefinedMappings casePredefinedMappings;

    public boolean isDefaultTextCaseData(FieldTypeDefinition fieldType) {
        return getTypeMappings().getDefaultText().contains(fieldType.getType())
            || isCollectionTypeDefaultText(fieldType);
    }

    public boolean isDefaultTextMetadata(String fieldId) {
        return getCasePredefinedMappings().getDefaultText().contains(fieldId);
    }

    private boolean isCollectionTypeDefaultText(FieldTypeDefinition fieldType) {
        return fieldType.getType().equals(FieldTypeDefinition.COLLECTION)
               && isDefaultTextCaseData(fieldType.getCollectionFieldTypeDefinition());
    }

    @AllArgsConstructor
    @Getter
    public static class TypeMappings {

        private final List<String> defaultText;
    }

    @AllArgsConstructor
    @Getter
    public static class CasePredefinedMappings {

        private final List<String> defaultText;
    }
}
