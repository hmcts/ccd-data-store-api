package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
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
    private final MidEventCallback midEventCallback;

    public AuthorisedValidateCaseFieldsOperation(AccessControlService accessControlService,
                                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                 CaseDefinitionRepository caseDefinitionRepository,
                                                 CaseAccessService caseAccessService,
                                                 @Qualifier(DefaultValidateCaseFieldsOperation.QUALIFIER)
                                                 ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                                 MidEventCallback midEventCallback) {
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.midEventCallback = midEventCallback;
    }

    @Override
    public Map<String, JsonNode> validateCaseDetails(OperationContext operationContext) {
        validateCaseFieldsOperation.validateCaseDetails(operationContext);

        CaseDataContent content = operationContext.content();
        String caseTypeId = operationContext.caseTypeId();
        String pageId = operationContext.pageId();

        final JsonNode data = midEventCallback.invoke(caseTypeId,
            content,
            pageId);

        content.setData(JacksonUtils.convertValue(data));

        verifyReadAccess(caseTypeId, content);

        return content.getData();
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition,
                             CaseDataContent content) {
        validateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);
    }

    private void verifyReadAccess(final String caseTypeId, CaseDataContent content) {
        final CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(caseTypeId);

        Set<AccessProfile> caseAccessProfiles =
            caseAccessService.getAccessProfilesByCaseReference(content.getCaseReference());

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            caseAccessProfiles,
            CAN_READ)) {
            content.setData(newHashMap());
            return;
        }

        content.setData(JacksonUtils.convertValueInDataField(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(content.getData().get(JacksonUtils.DATA)),
                caseTypeDefinition.getCaseFieldDefinitions(),
                caseAccessProfiles,
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
