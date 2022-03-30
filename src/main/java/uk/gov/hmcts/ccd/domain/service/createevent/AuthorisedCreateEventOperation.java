package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;
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
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessCategoriesService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.jsonpath.CaseDetailsJsonParser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.Optional;
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
    private final CaseAccessCategoriesService caseAccessCategoriesService;
    private final CaseDetailsJsonParser caseDetailsJsonParser;
    private final GetCaseOperation authGetCaseOperation;

    public AuthorisedCreateEventOperation(@Qualifier("classified") final CreateEventOperation createEventOperation,
                                          @Qualifier("default") final GetCaseOperation getCaseOperation,
                                          @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                          final CaseDefinitionRepository caseDefinitionRepository,
                                          final AccessControlService accessControlService,
                                          CaseAccessService caseAccessService,
                                          CaseAccessCategoriesService caseAccessCategoriesService,
                                          CaseDetailsJsonParser caseDetailsJsonParser,
                                          @Qualifier("authorised") final GetCaseOperation authGetCaseOperation) {

        this.createEventOperation = createEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.getCaseOperation = getCaseOperation;
        this.accessControlService = accessControlService;
        this.caseAccessService = caseAccessService;
        this.caseAccessCategoriesService = caseAccessCategoriesService;
        this.caseDetailsJsonParser = caseDetailsJsonParser;
        this.authGetCaseOperation = authGetCaseOperation;
    }

    @Override
    public CaseDetails createCaseEvent(String caseReference,
                                       CaseDataContent content) {

        CaseDetails existingCaseDetails = getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        Set<AccessProfile> accessProfiles = caseAccessService.getAccessProfilesByCaseReference(caseReference);
        if (accessProfiles == null || accessProfiles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        String caseTypeId = existingCaseDetails.getCaseTypeId();
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        verifyCaseAccessCategories(accessProfiles, existingCaseDetails);
        verifyUpsertAccess(content.getEvent(), content.getData(), existingCaseDetails,
            caseTypeDefinition, accessProfiles);

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseReference,
                                                                             content);
        return verifyReadAccess(caseTypeDefinition, accessProfiles, caseDetails);
    }

    @Override
    public CaseDetails createCaseSystemEvent(String caseReference,
                                             Integer version,
                                             String attributePath,
                                             String categoryId) {
        CreateCaseEventDetails createCaseEventDetails = getCreateCaseEventDetails(caseReference);

        //checks field denoted by attributePath exists and is of type document
        checkCaseDocumentData(attributePath, createCaseEventDetails);
        verifyUpsertAccessForCaseSystemEvent(createCaseEventDetails.getCaseDetails(),
            createCaseEventDetails.getCaseTypeDefinition(),
            createCaseEventDetails.getAccessProfiles());

        checkCaseCategoryId(createCaseEventDetails.getCaseTypeDefinition(), categoryId);

        if (!version.equals(createCaseEventDetails.getCaseDetails().getVersion())) {
            throw new BadRequestException("003 Wrong CaseVersion");
        }

        final CaseDetails caseDetails = createEventOperation.createCaseSystemEvent(caseReference,
            version, attributePath, categoryId);
        return verifyReadAccess(createCaseEventDetails.getCaseTypeDefinition(), createCaseEventDetails
            .getAccessProfiles(), caseDetails);
    }

    private void checkCaseCategoryId(CaseTypeDefinition caseTypeDefinition, String categoryId) {
        Boolean validCategoryId = categoryId == null || caseTypeDefinition.getCategories()
            .stream()
            .map(cfd -> cfd.getCategoryId())
            .filter(value -> StringUtils.isNotBlank(value))
            .anyMatch(value ->  value.equals(categoryId));
        if (!validCategoryId) {
            throw new BadRequestException("002 Invalid categoryId");
        }
    }

    private void checkCaseDocumentData(String attributePath, CreateCaseEventDetails createCaseEventDetails) {
        try {
            if (!caseDetailsJsonParser.containsDocumentUrl(createCaseEventDetails.getCaseDetails(), attributePath)) {
                throw new BadRequestException("Field denoted by path: '" + attributePath
                    + "' is not a document field type");
            }
        } catch (PathNotFoundException e) {
            throw  new BadRequestException("Field '" + attributePath + "' cannot be found");
        }
    }

    private CreateCaseEventDetails getCreateCaseEventDetails(String caseReference) {
        CreateCaseEventDetails createCaseEventDetails = new CreateCaseEventDetails();
        CaseDetails existingCaseDetails = authGetCaseOperation.execute(caseReference)
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

    private void verifyCaseAccessCategories(Set<AccessProfile> accessProfiles, CaseDetails existingCaseDetails) {
        Optional<CaseDetails> filteredCaseDetails = Optional.of(existingCaseDetails)
            .filter(caseAccessCategoriesService.caseHasMatchingCaseAccessCategories(accessProfiles, false));
        if (filteredCaseDetails.isEmpty()) {
            throw new CaseNotFoundException(existingCaseDetails.getReferenceAsString());
        }
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

    private void verifyUpsertAccessForCaseSystemEvent(CaseDetails existingCaseDetails,
                                                      CaseTypeDefinition caseTypeDefinition,
                                                      Set<AccessProfile> accessProfiles) {
        verifyCaseTypeAndStateAccess(existingCaseDetails, caseTypeDefinition, accessProfiles);

        verifyCaseFieldsAccess(existingCaseDetails.getData(), existingCaseDetails, caseTypeDefinition, accessProfiles);
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
