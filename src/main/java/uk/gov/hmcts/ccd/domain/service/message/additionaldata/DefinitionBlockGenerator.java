package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.config.MessagingProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DOCUMENT;
import static uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlock.DOCUMENT_TYPE_DEFINITION;

@Component
public class DefinitionBlockGenerator {

    private final MessagingProperties messagingProperties;

    @Autowired
    public DefinitionBlockGenerator(MessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    public Map<String, DefinitionBlock> generateDefinition(AdditionalDataContext context) {
        Map<String, DefinitionBlock> definition = newHashMap();

        context.getTopLevelPublishables().forEach(publishableField -> definition.put(publishableField.getKey(),
            buildDefinitionBlock(publishableField, context.getNestedPublishables(), true)));

        return definition;
    }

    private DefinitionBlock buildDefinitionBlock(PublishableField publishableField,
                                                 List<PublishableField> nestedPublishables,
                                                 boolean topLevel) {
        return DefinitionBlock.builder()
            .originalId(topLevel ? publishableField.getOriginalId() : publishableField.getFieldId())
            .subtype(getSubtype(publishableField.getFieldType()))
            .type(getType(publishableField.getFieldType()))
            .typeDef(buildTypeDefinition(publishableField, nestedPublishables,
                publishableField.getDisplayContext() == DisplayContext.COMPLEX || !topLevel))
            .build();
    }

    /**
     * Generates a DefinitionBlock for a field and ALL its children.
     */
    private DefinitionBlock buildDefinitionBlock(CommonField caseField) {
        return DefinitionBlock.builder()
            .originalId(caseField.getId())
            .subtype(getSubtype(caseField.getFieldTypeDefinition()))
            .type(getType(caseField.getFieldTypeDefinition()))
            .typeDef(buildTypeDefinition(caseField))
            .build();
    }

    private Map<String, DefinitionBlock> buildTypeDefinition(PublishableField publishableField,
                                                             List<PublishableField> nestedPublishables,
                                                             boolean filterSubFields) {
        if (DOCUMENT.equals(publishableField.getFieldType().getType())) {
            return DOCUMENT_TYPE_DEFINITION;
        }

        List<CaseFieldDefinition> childrenCaseFields = publishableField.getFieldType().getChildren();

        if (childrenCaseFields.isEmpty()) {
            return null;
        }

        Map<String, DefinitionBlock> typeDef = newHashMap();
        if (filterSubFields) {
            buildFilteredTypeDefinition(publishableField, nestedPublishables, typeDef);
        } else {
            childrenCaseFields.forEach(field -> typeDef.put(field.getId(), buildDefinitionBlock(field)));
        }

        return typeDef;
    }

    /**
     * Generates a type definition for ALL the children of a field.
     */
    private Map<String, DefinitionBlock> buildTypeDefinition(CommonField caseField) {
        if (DOCUMENT.equals(caseField.getFieldTypeDefinition().getType())) {
            return DOCUMENT_TYPE_DEFINITION;
        }

        List<CaseFieldDefinition> childrenCaseFields = caseField.getFieldTypeDefinition().getChildren();

        if (childrenCaseFields.isEmpty()) {
            return null;
        }

        Map<String, DefinitionBlock> typeDef = newHashMap();
        childrenCaseFields.forEach(field ->
            typeDef.put(field.getId(), buildDefinitionBlock(field)));

        return typeDef;
    }

    private void buildFilteredTypeDefinition(PublishableField publishableField,
                                             List<PublishableField> nestedPublishables,
                                             Map<String, DefinitionBlock> typeDef) {
        List<PublishableField> allSubFields = nestedPublishables.stream()
            .filter(field -> field.isSubFieldOf(publishableField))
            .collect(Collectors.toList());

        List<PublishableField> directChildrenFields = publishableField.filterDirectChildrenFrom(allSubFields);

        directChildrenFields.forEach(field -> typeDef
            .put(field.getFieldId(), buildDefinitionBlock(field, allSubFields, false)));
    }

    private String getSubtype(FieldTypeDefinition fieldTypeDefinition) {
        if (fieldTypeDefinition.isDynamicFieldType()) {
            return null;
        }

        if (fieldTypeDefinition.isComplexFieldType()) {
            return fieldTypeDefinition.getId();
        }

        if (fieldTypeDefinition.isCollectionFieldType()) {
            FieldTypeDefinition collectionFieldType = fieldTypeDefinition.getCollectionFieldTypeDefinition();
            return collectionFieldType.isComplexFieldType()
                ? collectionFieldType.getId()
                : collectionFieldType.getType();
        }

        return fieldTypeDefinition.getType();
    }

    private String getType(FieldTypeDefinition fieldTypeDefinition) {
        return mappingOf(fieldTypeDefinition.getType());
    }

    public String mappingOf(String subtype) {
        return messagingProperties.getTypeMappings().getOrDefault(subtype, subtype);
    }
}
