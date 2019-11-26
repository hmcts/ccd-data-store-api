package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedCreateCaseOperation implements CreateCaseOperation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
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

        Set<String> userRoles = caseAccessService.getCaseCreationRoles();

        Event event = caseDataContent.getEvent();
        Map<String, JsonNode> data = caseDataContent.getData();
        String eventId = event == null ? null : event.getEventId();
        this.accessControlService.verifyCreateAccess(eventId, caseType, userRoles, MAPPER.convertValue(data, JsonNode.class));

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

}
