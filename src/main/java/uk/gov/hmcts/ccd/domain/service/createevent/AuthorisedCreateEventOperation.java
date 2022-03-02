package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CreateCaseEventDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

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
                                          @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                          final CaseDefinitionRepository caseDefinitionRepository,
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

        CreateCaseEventDetails createCaseEventDetails = getCreateCaseEventDetails(caseReference);

        verifyUpsertAccess(content.getEvent(), content.getData(), createCaseEventDetails.getCaseDetails(),
            createCaseEventDetails.getCaseTypeDefinition(), createCaseEventDetails.getAccessProfiles());



        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseReference,
                                                                             content);
        return verifyReadAccess(createCaseEventDetails.getCaseTypeDefinition(), createCaseEventDetails
            .getAccessProfiles(), caseDetails);
    }

    @Override
    public CaseDetails createCaseSystemEvent(String caseReference, CaseDataContent content, Integer version,
                                             String attributePath, String categoryId) {
        CreateCaseEventDetails createCaseEventDetails = getCreateCaseEventDetails(caseReference);

        //checks field denoted by attributePath exists and is of type document
        JsonNode data;
        if (attributePath.contains(".")) {
            List<String> paths = Arrays.asList(attributePath.split("\\."));
            String fieldName;
            if (paths.get(0).contains("[")) {
                int fieldNameEnd = paths.get(0).indexOf("[");
                fieldName = paths.get(0).substring(0, fieldNameEnd);
                data = createCaseEventDetails.getCaseDetails().getData().get(fieldName);
                if (data == null) {
                    throw new BadRequestException("Field '" + fieldName + "' cannot be found");
                }
                int idEnd = paths.get(0).indexOf("]");
                String id = paths.get(0).substring(fieldNameEnd + 1, idEnd);
                List<JsonNode> collectionData = data.findParents("id").stream().filter(x -> x.toString()
                    .contains("\"id\":\"" + id + "\"")).collect(Collectors.toList());
                if (collectionData.size() != 1) {
                    throw new BadRequestException("Id: '" + id + "' does not match field in " + fieldName);
                }
                data = collectionData.get(0).get("value");
            } else {
                data = createCaseEventDetails.getCaseDetails().getData().get(paths.get(0));
                if (data == null) {
                    throw new BadRequestException("Field '" + paths.get(0) + "' cannot be found");
                }
            }
            for (int i = 1; i < paths.size(); i++) {
                if (paths.get(i).contains("[")) {
                    int fieldNameEnd = paths.get(i).indexOf("[");
                    fieldName = paths.get(i).substring(0, fieldNameEnd);
                    data = data.get(fieldName);
                    if (data == null) {
                        throw new BadRequestException("Field '" + fieldName + "' cannot be found");
                    }
                    int idEnd = paths.get(i).indexOf("]");
                    String id = paths.get(i).substring(fieldNameEnd + 1, idEnd);
                    List<JsonNode> collectionData = data.findParents("id").stream().filter(x -> x.toString()
                        .contains("\"id\":\"" + id + "\"")).collect(Collectors.toList());
                    if (collectionData.size() != 1) {
                        throw new BadRequestException("Id: '" + id + "' does not match field in " + fieldName);
                    }
                    data = collectionData.get(0).get("value");
                } else {
                    fieldName = paths.get(i);
                    data = data.get(fieldName);
                    if (data == null) {
                        throw new BadRequestException("Field '" + fieldName + "' cannot be found");
                    }
                }
            }
        } else if (attributePath.contains("[")) {
            int fieldNameEnd = attributePath.indexOf("[");
            int idEnd = attributePath.indexOf("]");
            String fieldName = attributePath.substring(0, fieldNameEnd);
            data = createCaseEventDetails.getCaseDetails().getData().get(fieldName);
            if (data == null) {
                throw new BadRequestException("Field '" + fieldName + "' does not exist");
            }
            String id = attributePath.substring(fieldNameEnd + 1, idEnd);
            List<JsonNode> collectionData = data.findParents("id").stream().filter(x -> x.toString()
                .contains("\"id\":\"" + id + "\"")).collect(Collectors.toList());
            if (collectionData.size() != 1) {
                throw new BadRequestException("Id: '" + id + "' does not match field in " + fieldName);
            }
            data = collectionData.get(0).get("value");
        } else {
            data = createCaseEventDetails.getCaseDetails().getData().get(attributePath);
            if (data == null) {
                throw new BadRequestException("Field '" + attributePath + "' does not exist");
            }
        }
        data = data.get("document_url");
        if (data == null) {
            throw new BadRequestException("Field denoted by path: '" + attributePath
                + "' is not a document field type");
        }

        verifyUpsertAccessForCaseSystemEvent(content.getData(), createCaseEventDetails.getCaseDetails(),
            createCaseEventDetails.getCaseTypeDefinition(), createCaseEventDetails.getAccessProfiles());

        //categoryId check needed here

        if (!version.equals(createCaseEventDetails.getCaseDetails().getVersion())) {
            throw new BadRequestException("003 Wrong CaseVersion");
        }

        final CaseDetails caseDetails = createEventOperation.createCaseSystemEvent(caseReference,
            content, version, attributePath, categoryId);
        return verifyReadAccess(createCaseEventDetails.getCaseTypeDefinition(), createCaseEventDetails
            .getAccessProfiles(), caseDetails);
    }

    private CreateCaseEventDetails getCreateCaseEventDetails(String caseReference) {
        CreateCaseEventDetails createCaseEventDetails = new CreateCaseEventDetails();
        CaseDetails existingCaseDetails = getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
        createCaseEventDetails.setCaseDetails(existingCaseDetails);

        Set<AccessProfile> accessProfiles = caseAccessService.getAccessProfilesByCaseReference(caseReference);
        if (accessProfiles == null || accessProfiles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        createCaseEventDetails.setAccessProfiles(accessProfiles);

        String caseTypeId = existingCaseDetails.getCaseTypeId();
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        createCaseEventDetails.setCaseTypeDefinition(caseTypeDefinition);
        return createCaseEventDetails;
    }

    private CaseDetails verifyReadAccess(CaseTypeDefinition caseTypeDefinition,
                                         Set<AccessProfile> accessProfiles,
                                         CaseDetails caseDetails) {

        if (caseDetails != null) {
            if (!accessControlService.canAccessCaseTypeWithCriteria(
                caseTypeDefinition,
                accessProfiles,
                CAN_READ)) {
                return null;
            }

            caseDetails.setData(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    accessProfiles,
                    CAN_READ,
                    false)));

            caseDetails.setDataClassification(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    accessProfiles,
                    CAN_READ,
                    true)));
        }
        return caseDetails;
    }

    private void verifyUpsertAccess(Event event, Map<String, JsonNode> newData,
                                    CaseDetails existingCaseDetails,
                                    CaseTypeDefinition caseTypeDefinition,
                                    Set<AccessProfile> accessProfiles) {

        verifyCaseTypeAndStateAccess(existingCaseDetails, caseTypeDefinition, accessProfiles);

        if (event == null || !accessControlService.canAccessCaseEventWithCriteria(
            event.getEventId(),
            caseTypeDefinition.getEvents(),
            accessProfiles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        verifyCaseFieldsAccess(newData, existingCaseDetails, caseTypeDefinition, accessProfiles);
    }

    private void verifyUpsertAccessForCaseSystemEvent(Map<String, JsonNode> newData, CaseDetails existingCaseDetails,
                                                      CaseTypeDefinition caseTypeDefinition,
                                                      Set<AccessProfile> accessProfiles) {
        verifyCaseTypeAndStateAccess(existingCaseDetails, caseTypeDefinition, accessProfiles);

        verifyCaseFieldsAccess(newData, existingCaseDetails, caseTypeDefinition, accessProfiles);
    }

    private void verifyCaseTypeAndStateAccess(CaseDetails existingCaseDetails, CaseTypeDefinition caseTypeDefinition,
                                              Set<AccessProfile> accessProfiles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(existingCaseDetails.getState(), caseTypeDefinition,
            accessProfiles, CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }
    }

    private void verifyCaseFieldsAccess(Map<String, JsonNode> newData, CaseDetails existingCaseDetails,
                                        CaseTypeDefinition caseTypeDefinition, Set<AccessProfile> accessProfiles) {
        if (!accessControlService.canAccessCaseFieldsForUpsert(
            MAPPER.convertValue(newData, JsonNode.class),
            MAPPER.convertValue(existingCaseDetails.getData(), JsonNode.class),
            caseTypeDefinition.getCaseFieldDefinitions(),
            accessProfiles)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }

}
