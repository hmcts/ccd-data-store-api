package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;

@Service
@Qualifier("authorised")
public class AuthorisedCreateEventOperation implements CreateEventOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final CreateEventOperation createEventOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final GetCaseOperation getCaseOperation;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;

    public AuthorisedCreateEventOperation(@Qualifier("classified") final CreateEventOperation createEventOperation,
                                          @Qualifier("default") final GetCaseOperation getCaseOperation,
                                          @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                          final AccessControlService accessControlService,
                                          @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {

        this.createEventOperation = createEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.getCaseOperation = getCaseOperation;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
    }

    @Override
    public CaseDetails createCaseEvent(String uid,
                                       String jurisdictionId,
                                       String caseTypeId,
                                       String caseReference,
                                       Event event,
                                       Map<String, JsonNode> data,
                                       String token,
                                       Boolean ignoreWarning) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        CaseDetails existingCaseDetails = getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        verifyUpsertAccess(event, data, existingCaseDetails, caseType, userRoles);

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(uid,
                                                                             jurisdictionId,
                                                                             caseTypeId,
                                                                             caseReference,
                                                                             event,
                                                                             data,
                                                                             token,
                                                                             ignoreWarning);
        return verifyReadAccess(caseType, userRoles, caseDetails);
    }

    private CaseDetails verifyReadAccess(CaseType caseType, Set<String> userRoles, CaseDetails caseDetails) {

        if (caseDetails != null) {
            if (!accessControlService.canAccessCaseTypeWithCriteria(
                                caseType,
                                userRoles,
                                CAN_READ)) {
                return null;
            }

            caseDetails.setData(MAPPER.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ),
                STRING_JSON_MAP));
            caseDetails.setDataClassification(MAPPER.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ),
                STRING_JSON_MAP));
        }
        return caseDetails;
    }

    private void verifyUpsertAccess(Event event, Map<String, JsonNode> newData, CaseDetails existingCaseDetails, CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,userRoles,CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(existingCaseDetails.getState(), caseType, userRoles, CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }

        if (event == null || !accessControlService.canAccessCaseEventWithCriteria(
                            event.getEventId(),
                            caseType.getEvents(),
                            userRoles,
                            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        if (!accessControlService.canAccessCaseFieldsForUpsert(
            MAPPER.convertValue(newData, JsonNode.class),
            MAPPER.convertValue(existingCaseDetails.getData(), JsonNode.class),
            caseType.getCaseFields(),
            userRoles)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }
}
