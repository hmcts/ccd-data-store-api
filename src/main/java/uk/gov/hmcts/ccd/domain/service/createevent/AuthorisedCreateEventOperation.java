package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_STATE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_EVENT_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_FIELD_FOUND;

@Service
@Qualifier("authorised")
public class AuthorisedCreateEventOperation implements CreateEventOperation {


    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
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
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        verifyUpsertAccess(content.getEvent(), content.getData(), existingCaseDetails, caseType, userRoles);

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseReference,
            content);
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

            caseDetails.setData(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ,
                    false)));

            caseDetails.setDataClassification(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ,
                    true)));
        }
        return caseDetails;
    }

    private void verifyUpsertAccess(Event event, Map<String, JsonNode> newData, CaseDetails existingCaseDetails, CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_UPDATE)) {
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
            JacksonUtils.convertValueJsonNode(newData),
            JacksonUtils.convertValueJsonNode(existingCaseDetails.getData()),
            caseType.getCaseFields(),
            userRoles)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }
}
