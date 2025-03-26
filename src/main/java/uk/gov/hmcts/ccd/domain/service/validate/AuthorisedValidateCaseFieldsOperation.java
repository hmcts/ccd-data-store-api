package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    public AuthorisedValidateCaseFieldsOperation(AccessControlService accessControlService,
                                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                 CaseDefinitionRepository caseDefinitionRepository,
                                                 CaseAccessService caseAccessService,
                                                 @Qualifier(ClassifiedValidateCaseFieldsOperation.QUALIFIER)
                                                 ValidateCaseFieldsOperation validateCaseFieldsOperation) {
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
    }

    @Override
    public Map<String, JsonNode> validateCaseDetails(OperationContext operationContext) {
        validateCaseFieldsOperation.validateCaseDetails(operationContext);

        CaseDataContent content = operationContext.content();
        String caseTypeId = operationContext.caseTypeId();

        String caseReference = content.getCaseReference();
        Set<AccessProfile> accessProfiles = StringUtils.isNotEmpty(caseReference)
            ? caseAccessService.getAccessProfilesByCaseReference(caseReference) :
            caseAccessService.getCaseCreationRoles(caseTypeId);

        verifyReadAccess(caseTypeId, content, accessProfiles);

        return content.getData();
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition,
                             CaseDataContent content) {
        validateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);
    }

    private void verifyReadAccess(final String caseTypeId, CaseDataContent content, Set<AccessProfile> accessProfiles) {
        final CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(caseTypeId);

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

        content.setData(JacksonUtils.convertValueInDataField(
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
