package uk.gov.hmcts.ccd.domain.service.createcase;

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
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;

@Service
@Qualifier("authorised")
public class AuthorisedCreateCaseOperation implements CreateCaseOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final CreateCaseOperation createCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;

    public AuthorisedCreateCaseOperation(@Qualifier("classified") final CreateCaseOperation createCaseOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         final AccessControlService accessControlService,
                                         @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {

        this.createCaseOperation = createCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;

    }

    @Override
    public CaseDetails createCaseDetails(final String uid,
                                         String jurisdictionId,
                                         String caseTypeId,
                                         CaseDataContent caseDataContent,
                                         Boolean ignoreWarning) {
        if (caseDataContent == null) {
            throw new ValidationException("No data provided");
        }

        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        Event event = caseDataContent.getEvent();
        Map<String, JsonNode> data = caseDataContent.getData();
        verifyCreateAccess(event, data, caseType, userRoles);

        final CaseDetails caseDetails = createCaseOperation.createCaseDetails(uid,
                                                                              jurisdictionId,
                                                                              caseTypeId,
                                                                              caseDataContent,
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
                    CAN_READ, false),
                STRING_JSON_MAP));
            caseDetails.setDataClassification(MAPPER.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ,
                    true),
                STRING_JSON_MAP));
        }
        return caseDetails;
    }

    private void verifyCreateAccess(Event event, Map<String, JsonNode> data, CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseType,
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }

        if (event == null || !accessControlService.canAccessCaseEventWithCriteria(
            event.getEventId(),
            caseType.getEvents(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        if (!accessControlService.canAccessCaseFieldsWithCriteria(
            MAPPER.convertValue(data, JsonNode.class),
            caseType.getCaseFields(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }
}
