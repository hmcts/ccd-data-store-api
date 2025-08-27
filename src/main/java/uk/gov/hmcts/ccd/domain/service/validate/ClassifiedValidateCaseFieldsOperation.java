package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Optional.ofNullable;

@Service
@Qualifier(ClassifiedValidateCaseFieldsOperation.QUALIFIER)
public class ClassifiedValidateCaseFieldsOperation implements ValidateCaseFieldsOperation {
    public static final String QUALIFIER = "classified";

    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final MidEventCallback midEventCallback;
    private final CaseDataService caseDataService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseService caseService;



    public ClassifiedValidateCaseFieldsOperation(@Qualifier(DefaultValidateCaseFieldsOperation.QUALIFIER)
                                                 ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                                 SecurityClassificationServiceImpl classificationService,
                                                 MidEventCallback midEventCallback, CaseDataService caseDataService,
                                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                 CaseDefinitionRepository caseDefinitionRepository,
                                                 CaseService caseService) {
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.classificationService = classificationService;
        this.midEventCallback = midEventCallback;
        this.caseDataService = caseDataService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseService = caseService;
    }

    @Override
    public Map<String, JsonNode> validateCaseDetails(OperationContext operationContext) {
        validateCaseFieldsOperation.validateCaseDetails(operationContext);

        CaseDataContent content = operationContext.content();
        String caseTypeId = operationContext.caseTypeId();
        String pageId = operationContext.pageId();


        final Map<String, JsonNode> callbackData = midEventCallback.invoke(caseTypeId,
            content,
            pageId);

        CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(caseTypeId);

        CaseGenerationResult caseDetailsAndIsCreate = generateOrRetrieveCaseDetails(operationContext,
            caseTypeDefinition, callbackData);

        CaseDetails classifiedCaseDetails =
            classificationService.applyClassification(caseDetailsAndIsCreate.caseDetails(),
                caseDetailsAndIsCreate.isCreate()).orElse(new CaseDetails());

        content.setData(classifiedCaseDetails.getData());

        return content.getData();
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition,
                             CaseDataContent content) {
        validateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);
    }

    private CaseGenerationResult generateOrRetrieveCaseDetails(OperationContext operationContext,
                                                               CaseTypeDefinition caseTypeDefinition,
                                                               Map<String, JsonNode> callbackData) {
        CaseDetails caseDetails;
        boolean isCreate;
        if (StringUtils.isNotEmpty(operationContext.content().getCaseReference())) {
            isCreate = false;
            caseDetails =
                caseService.getCaseDetails(caseTypeDefinition.getJurisdictionId(),
                    operationContext.content().getCaseReference());
        } else {
            isCreate = true;
            caseDetails = new CaseDetails();
            caseDetails.setCaseTypeId(operationContext.caseTypeId());
            caseDetails.setSecurityClassification(caseTypeDefinition.getSecurityClassification());
        }

        CaseDetails caseDetailsClone = caseService.clone(caseDetails);
        caseDetailsClone.setData(callbackData);
        deduceDataClassificationForNewFields(caseTypeDefinition, caseDetailsClone);

        return new CaseGenerationResult(caseDetailsClone, isCreate);
    }

    private CaseTypeDefinition getCaseDefinitionType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    private void deduceDataClassificationForNewFields(CaseTypeDefinition caseTypeDefinition, CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
            caseDetails.getData(),
            ofNullable(caseDetails.getDataClassification()).orElse(
                newHashMap()));
        caseDetails.setDataClassification(defaultSecurityClassifications);
    }

    private record CaseGenerationResult(CaseDetails caseDetails, boolean isCreate) {}

}
