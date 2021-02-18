package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import lombok.Getter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.service.message.additionaldata.PublishableField.FIELD_SEPARATOR;

@Getter
public class AdditionalDataContext {

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;
    private List<PublishableField> publishableFields;
    private List<PublishableField> topLevelPublishables;
    private List<PublishableField> nestedPublishables;

    public AdditionalDataContext(MessageContext messageContext) {
        this(messageContext.getCaseEventDefinition(),
            messageContext.getCaseTypeDefinition(),
            messageContext.getCaseDetails());
    }

    public AdditionalDataContext(CaseEventDefinition caseEventDefinition,
                                 CaseTypeDefinition caseTypeDefinition,
                                 CaseDetails caseDetails) {
        this.caseEventDefinition = caseEventDefinition;
        this.caseTypeDefinition = caseTypeDefinition;
        this.caseDetails = caseDetails;
        this.setPublishableFields(caseEventDefinition, caseTypeDefinition, caseDetails);
    }

    private void setPublishableFields(CaseEventDefinition caseEventDefinition,
                                      CaseTypeDefinition caseTypeDefinition,
                                      CaseDetails caseDetails) {
        this.publishableFields = findPublishableFields(caseEventDefinition, caseTypeDefinition, caseDetails);
        this.topLevelPublishables = publishableFields.stream()
            .filter(PublishableField::isPublishTopLevel)
            .collect(toList());
        this.nestedPublishables = publishableFields.stream()
            .filter(PublishableField::isNested)
            .collect(toList());
    }

    private List<PublishableField> findPublishableFields(CaseEventDefinition caseEventDefinition,
                                                         CaseTypeDefinition caseTypeDefinition,
                                                         CaseDetails caseDetails) {
        List<PublishableField> fields = newArrayList();

        caseEventDefinition.getCaseFields().forEach(caseEventField -> {
            if (caseEventField.getDisplayContextEnum() == DisplayContext.COMPLEX) {
                fields.addAll(findPublishableNestedFields(caseTypeDefinition, caseEventField));
            } else if (Boolean.TRUE.equals(caseEventField.getPublish())) {
                fields.add(new PublishableField(caseTypeDefinition, caseEventField));
            }
        });

        return fields;
    }

    private List<PublishableField> findPublishableNestedFields(CaseTypeDefinition caseTypeDefinition,
                                                               CaseEventFieldDefinition caseEventField) {
        List<PublishableField> fields = newArrayList();

        caseEventField.getCaseEventFieldComplexDefinitions().forEach(caseEventFieldComplex -> {
            if (Boolean.TRUE.equals(caseEventFieldComplex.getPublish())) {
                Optional<PublishableField> existingParentField = fields.stream()
                    .filter(field -> field.getCaseField().getId().equals(caseEventField.getCaseFieldId()))
                    .findFirst();

                if (existingParentField.isEmpty()) {
                    fields.add(new PublishableField(caseTypeDefinition, caseEventField));
                }

                String path = caseEventField.getCaseFieldId() + FIELD_SEPARATOR + caseEventFieldComplex.getReference();
                fields.add(new PublishableField(caseTypeDefinition, caseEventFieldComplex, path));
            }
        });

        return fields;
    }
}
