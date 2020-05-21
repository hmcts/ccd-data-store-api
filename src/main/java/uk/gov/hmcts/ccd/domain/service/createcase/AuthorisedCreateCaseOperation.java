package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_EVENT_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_FIELD_FOUND;

@Service
@Qualifier("authorised")
public class AuthorisedCreateCaseOperation implements CreateCaseOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final CreateCaseOperation createCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final CaseAccessService caseAccessService;


    public AuthorisedCreateCaseOperation(@Qualifier("classified") final CreateCaseOperation createCaseOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         final AccessControlService accessControlService,
                                         final CaseAccessService caseAccessService) {

        this.createCaseOperation = createCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.caseAccessService = caseAccessService;

    }

    @Override
    public CaseDetails createCaseDetails(String caseTypeId,
                                         CaseDataContent caseDataContent,
                                         Boolean ignoreWarning) {
        if (caseDataContent == null) {
            throw new ValidationException("No data provided");
        }

        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        Set<String> userRoles = caseAccessService.getCaseCreationRoles();

        Event event = caseDataContent.getEvent();
        Map<String, JsonNode> data = caseDataContent.getData();
        verifyCreateAccess(event, data, caseTypeDefinition, userRoles);

        final CaseDetails caseDetails = createCaseOperation.createCaseDetails(caseTypeId,
                                                                              caseDataContent,
                                                                              ignoreWarning);
        return verifyReadAccess(caseTypeDefinition, userRoles, caseDetails);
    }

    private CaseDetails verifyReadAccess(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles, CaseDetails caseDetails) {

        if (caseDetails != null) {
            if (!accessControlService.canAccessCaseTypeWithCriteria(
                caseTypeDefinition,
                userRoles,
                CAN_READ)) {
                return null;
            }

            caseDetails.setData(MAPPER.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    userRoles,
                    CAN_READ, false),
                STRING_JSON_MAP));
            caseDetails.setDataClassification(MAPPER.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    userRoles,
                    CAN_READ,
                    true),
                STRING_JSON_MAP));
        }
        return caseDetails;
    }

    private void verifyCreateAccess(Event event, Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }

        if (event == null || !accessControlService.canAccessCaseEventWithCriteria(
            event.getEventId(),
            caseTypeDefinition.getEvents(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        if (!accessControlService.canAccessCaseFieldsWithCriteria(
            MAPPER.convertValue(data, JsonNode.class),
            caseTypeDefinition.getCaseFieldDefinitions(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }
}
