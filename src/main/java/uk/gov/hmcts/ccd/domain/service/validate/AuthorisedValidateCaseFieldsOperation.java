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
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.ConditionalFieldRestorer;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_STATE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_EVENT_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_FIELD_FOUND;

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
    private final GetCaseOperation getCaseOperation;

    public AuthorisedValidateCaseFieldsOperation(AccessControlService accessControlService,
                                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                 CaseDefinitionRepository caseDefinitionRepository,
                                                 CaseAccessService caseAccessService,
                                                 @Qualifier(DefaultValidateCaseFieldsOperation.QUALIFIER)
                                                 ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                                 ConditionalFieldRestorer conditionalFieldRestorer,
                                                 ApplicationParams applicationParams,
                                                 MidEventCallback midEventCallback,
                                                 @Qualifier("default") GetCaseOperation getCaseOperation) {
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.conditionalFieldRestorer = conditionalFieldRestorer;
        this.applicationParams = applicationParams;
        this.midEventCallback = midEventCallback;
        this.getCaseOperation = getCaseOperation;
    }

    @Override
    public Map<String, JsonNode> validateCaseDetails(OperationContext operationContext) {
        validateCaseFieldsOperation.validateCaseDetails(operationContext);

        CaseDataContent content = operationContext.content();
        String caseTypeId = operationContext.caseTypeId();
        String pageId = operationContext.pageId();

        verifyEventAccessBeforeMidEvent(operationContext);

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

    private void verifyEventAccessBeforeMidEvent(OperationContext operationContext) {
        CaseDataContent content = operationContext.content();
        String caseTypeId = operationContext.caseTypeId();

        Event event = content.getEvent();
        if (event == null || StringUtils.isEmpty(event.getEventId())) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        final CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(caseTypeId);

        if (StringUtils.isEmpty(content.getCaseReference())) {
            verifyCreateCaseEventAccess(content, caseTypeDefinition);
        } else {
            verifyUpdateCaseEventAccess(content, caseTypeDefinition);
        }
    }

    private void verifyCreateCaseEventAccess(CaseDataContent content, CaseTypeDefinition caseTypeDefinition) {
        Set<AccessProfile> userRoles = caseAccessService.getCaseCreationRoles(caseTypeDefinition.getId());
        if (userRoles == null || userRoles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseEventWithCriteria(
            content.getEvent().getEventId(),
            caseTypeDefinition.getEvents(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
        if (!accessControlService.canAccessCaseFieldsWithCriteria(
            JacksonUtils.convertValueJsonNode(content.getData() == null ? Map.of() : content.getData()),
            caseTypeDefinition.getCaseFieldDefinitions(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }

    private void verifyUpdateCaseEventAccess(CaseDataContent content, CaseTypeDefinition caseTypeDefinition) {
        String caseReference = content.getCaseReference();
        CaseDetails existingCaseDetails = getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        Set<AccessProfile> accessProfiles = caseAccessService.getAccessProfilesByCaseReference(caseReference);
        if (accessProfiles == null || accessProfiles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        verifyCaseTypeAndStateAccessForUpdate(existingCaseDetails, caseTypeDefinition, accessProfiles);

        if (!accessControlService.canAccessCaseEventWithCriteria(
            content.getEvent().getEventId(),
            caseTypeDefinition.getEvents(),
            accessProfiles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
    }

    private void verifyCaseTypeAndStateAccessForUpdate(CaseDetails existingCaseDetails,
                                                       CaseTypeDefinition caseTypeDefinition,
                                                       Set<AccessProfile> accessProfiles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(
            existingCaseDetails.getState(),
            caseTypeDefinition,
            accessProfiles,
            CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }
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
