package uk.gov.hmcts.ccd.domain.service.createevent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier("authorised")
public class AuthorisedCreateEventOperation implements CreateEventOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final CreateEventOperation createEventOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final GetCaseOperation getCaseOperation;
    private final AccessControlService accessControlService;
    private final CaseAccessService caseAccessService;

    public AuthorisedCreateEventOperation(@Qualifier("classified") final CreateEventOperation createEventOperation,
                                          @Qualifier("default") final GetCaseOperation getCaseOperation,
                                          @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                          final AccessControlService accessControlService,
                                          CaseAccessService caseAccessService) {

        this.createEventOperation = createEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.getCaseOperation = getCaseOperation;
        this.accessControlService = accessControlService;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public CaseDetails createCaseEvent(String caseReference,
                                       CaseDataContent content) {

        CaseDetails existingCaseDetails = getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        Set<String> userRoles = Sets.union(caseAccessService.getUserRoles(),
            caseAccessService.getCaseRoles(existingCaseDetails.getId()));
        if (userRoles == null || userRoles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        String caseTypeId = existingCaseDetails.getCaseTypeId();
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        verifyUpsertAccess(content.getEvent(), content.getData(), existingCaseDetails, caseTypeDefinition, userRoles);

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseReference,
                                                                             content);
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
                    CAN_READ,
                    false),
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

    private void verifyUpsertAccess(Event event, Map<String, JsonNode> newData, CaseDetails existingCaseDetails, CaseTypeDefinition caseTypeDefinition, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,userRoles,CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(existingCaseDetails.getState(), caseTypeDefinition, userRoles, CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }

        if (event == null || !accessControlService.canAccessCaseEventWithCriteria(
                            event.getEventId(),
                            caseTypeDefinition.getEvents(),
                            userRoles,
                            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        if (!accessControlService.canAccessCaseFieldsForUpsert(
            MAPPER.convertValue(newData, JsonNode.class),
            MAPPER.convertValue(existingCaseDetails.getData(), JsonNode.class),
            caseTypeDefinition.getCaseFieldDefinitions(),
            userRoles)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }
}
