package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedStartEventOperation implements StartEventOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final StartEventOperation startEventOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final AccessControlService accessControlService;
    private final UIDService uidService;
    private final UserRepository userRepository;
    private final DraftGateway draftGateway;

    public AuthorisedStartEventOperation(@Qualifier("classified") final StartEventOperation startEventOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                         final AccessControlService accessControlService,
                                         final UIDService uidService,
                                         @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                         @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway) {

        this.startEventOperation = startEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.accessControlService = accessControlService;
        this.uidService = uidService;
        this.userRepository = userRepository;
        this.draftGateway = draftGateway;
    }

    @Override
    public StartEventTrigger triggerStartForCaseType(String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                        eventTriggerId,
                                                                                        ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForCase(String caseReference, String eventTriggerId, Boolean ignoreWarning) {

        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return caseDetailsRepository.findByReference(caseReference)
            .map(caseDetails -> verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForCase(caseReference,
                                                                                                                      eventTriggerId,
                                                                                                                      ignoreWarning)))
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    @Override
    public StartEventTrigger triggerStartForDraft(String draftReference,
                                                  Boolean ignoreWarning) {

        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForDraft(draftReference,
                                                                                                      ignoreWarning));
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseType;
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

        Set<String> userRoles = getUserRoles();

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
