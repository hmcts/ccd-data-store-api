package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import lombok.Getter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Getter
public class AdditionalDataContext {

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;
    private List<PublishableField> publishableFields;
    private List<PublishableField> topLevelPublishables;
    private List<PublishableField> nestedPublishables;

    public AdditionalDataContext(CaseEventDefinition caseEventDefinition,
                                 CaseTypeDefinition caseTypeDefinition, CaseDetails caseDetails) {
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
            .collect(Collectors.toList());
        this.nestedPublishables = publishableFields.stream()
            .filter(PublishableField::isNested)
            .collect(Collectors.toList());
    }

    private List<PublishableField> findPublishableFields(CaseEventDefinition caseEventDefinition,
                                                         CaseTypeDefinition caseTypeDefinition,
                                                         CaseDetails caseDetails) {

        List<PublishableField> fields = newArrayList();
        caseEventDefinition.getCaseFields().forEach(caseEventField -> {
            if (Boolean.TRUE.equals(caseEventField.getPublish())) {
                fields.add(new PublishableField(caseTypeDefinition, caseEventField, caseDetails));

                if (caseEventField.getDisplayContextEnum() == DisplayContext.COMPLEX) {
                    caseEventField.getCaseEventFieldComplexDefinitions().forEach(caseEventFieldComplex -> {
                        if (Boolean.TRUE.equals(caseEventFieldComplex.getPublish())) {
                            String path = caseEventField.getCaseFieldId() + "." + caseEventFieldComplex.getReference();
                            fields.add(new PublishableField(caseTypeDefinition, caseEventFieldComplex, path, caseDetails));
                        }
                    });
                }
            }
        });
        return fields;
    }
}
