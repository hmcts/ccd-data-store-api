package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.ConditionalFieldRestorer;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Slf4j
@Qualifier(AuthorisedValidateCaseFieldsOperation.QUALIFIER)
public class AuthorisedValidateCaseFieldsOperation implements ValidateCaseFieldsOperation {
    public static final String QUALIFIER = "authorised";

    private final AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAccessService caseAccessService;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final ConditionalFieldRestorer conditionalFieldRestorer;
    private final ApplicationParams applicationParams;
    private final MidEventCallback midEventCallback;

    public AuthorisedValidateCaseFieldsOperation(AccessControlService accessControlService,
                                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                 CaseDefinitionRepository caseDefinitionRepository,
                                                 CaseAccessService caseAccessService,
                                                 @Qualifier(DefaultValidateCaseFieldsOperation.QUALIFIER)
                                                 ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                                 ConditionalFieldRestorer conditionalFieldRestorer,
                                                 ApplicationParams applicationParams,
                                                 MidEventCallback midEventCallback) {
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.conditionalFieldRestorer = conditionalFieldRestorer;
        this.applicationParams = applicationParams;
        this.midEventCallback = midEventCallback;
    }

    @Override
    public Map<String, JsonNode> validateCaseDetails(OperationContext operationContext) {
        validateCaseFieldsOperation.validateCaseDetails(operationContext);

        CaseDataContent content = operationContext.content();
        String caseTypeId = operationContext.caseTypeId();
        String pageId = operationContext.pageId();

        callMidEventCallback(caseTypeId, content, pageId);

        if (applicationParams.getExcludeVerifyAccessCaseTypesForValidate()
            .stream()
            .anyMatch(c -> c.equalsIgnoreCase(caseTypeId))) {
            content.setData(JacksonUtils.convertValueInDataField(content.getData()));
            return content.getData();
        }

        Set<AccessProfile> accessProfiles = determineAccessProfiles(caseTypeId, content.getCaseReference());
        CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(caseTypeId);
        Map<String, JsonNode> validatedData = captureValidatedData(content);

        verifyReadAccess(caseTypeDefinition, content, accessProfiles);

        Map<String, JsonNode> restoredData = restoreConditionalFieldsData(
            caseTypeDefinition,
            content.getData(),
            validatedData,
            accessProfiles
        );

        content.setData(JacksonUtils.convertValueInDataField(restoredData));
        return content.getData();
    }

    private void callMidEventCallback(String caseTypeId, CaseDataContent content, String pageId) {
        content.setData(midEventCallback.invoke(caseTypeId, content, pageId));
    }

    private Set<AccessProfile> determineAccessProfiles(String caseTypeId, String caseReference) {
        return StringUtils.isNotEmpty(caseReference)
            ? caseAccessService.getAccessProfilesByCaseReference(caseReference)
            : caseAccessService.getCaseCreationRoles(caseTypeId);
    }

    private Map<String, JsonNode> captureValidatedData(CaseDataContent content) {
        return JacksonUtils.convertValue(
            JacksonUtils.convertValueJsonNode(content.getData())
        );
    }

    private Map<String, JsonNode> restoreConditionalFieldsData(
        CaseTypeDefinition caseTypeDefinition,
        Map<String, JsonNode> filteredData,
        Map<String, JsonNode> validatedData,
        Set<AccessProfile> accessProfiles
    ) {
        return conditionalFieldRestorer.restoreConditionalFields(
            caseTypeDefinition,
            filteredData,
            validatedData,
            accessProfiles
        );
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition,
                             CaseDataContent content) {
        validateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);
    }

    private void verifyReadAccess(CaseTypeDefinition caseTypeDefinition, CaseDataContent content,
                                  Set<AccessProfile> accessProfiles) {
        if (content.getData() == null) {
            content.setData(newHashMap());
            return;
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            accessProfiles,
            CAN_READ)) {
            content.setData(newHashMap());
            return;
        }

        content.setData(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(content.getData()),
                caseTypeDefinition.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                false)));
    }

    private CaseTypeDefinition getCaseDefinitionType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }
}
