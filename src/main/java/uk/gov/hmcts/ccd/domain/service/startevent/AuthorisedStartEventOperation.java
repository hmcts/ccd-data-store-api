package uk.gov.hmcts.ccd.domain.service.startevent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier("authorised")
public class AuthorisedStartEventOperation implements StartEventOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final StartEventOperation startEventOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseAccessService caseAccessService;

    public AuthorisedStartEventOperation(@Qualifier("classified") final StartEventOperation startEventOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         final AccessControlService accessControlService,
                                         @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                         CaseAccessService caseAccessService) {

        this.startEventOperation = startEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public StartEventTrigger triggerStartForCaseType(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForCaseType(uid,
                                                                                        jurisdictionId,
                                                                                        caseTypeId,
                                                                                        eventTriggerId,
                                                                                        ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForCase(String uid, String jurisdictionId, String caseTypeId, String caseReference, String eventTriggerId, Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForCase(uid,
                                                                                    jurisdictionId,
                                                                                    caseTypeId,
                                                                                    caseReference,
                                                                                    eventTriggerId,
                                                                                    ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForDraft(String uid, String jurisdictionId, String caseTypeId, String draftReference, String eventTriggerId,
                                                  Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForDraft(uid,
                                                                                     jurisdictionId,
                                                                                     caseTypeId,
                                                                                     draftReference,
                                                                                     eventTriggerId,
                                                                                     ignoreWarning));
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseType;
    }

    private Set<String> getCaseRoles(CaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getId() == null) {
            return Collections.EMPTY_SET;
        } else {
            return caseAccessService.getCaseRoles(caseDetails.getId());
        }
    }


    private Set<String> getUserRoles() {
        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        return userRoles;
    }

    private StartEventTrigger verifyReadAccess(final String caseTypeId, final StartEventTrigger startEventTrigger) {

        final CaseType caseType = getCaseType(caseTypeId);

        Set<String> userRoles = Sets.union(getUserRoles(), getCaseRoles(startEventTrigger.getCaseDetails()));

        CaseDetails caseDetails = startEventTrigger.getCaseDetails();

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseType,
            userRoles,
            CAN_READ)) {
            caseDetails.setData(newHashMap());
            caseDetails.setDataClassification(newHashMap());
            return startEventTrigger;
        }

        if (caseDetails != null) {
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
        return startEventTrigger;
    }


}
