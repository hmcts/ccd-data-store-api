package uk.gov.hmcts.ccd.domain.service.startevent;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedStartEventOperation implements StartEventOperation {

    private final StartEventOperation startEventOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final AccessControlService accessControlService;
    private final UIDService uidService;
    private final CaseAccessService caseAccessService;
    private final DraftGateway draftGateway;

    public AuthorisedStartEventOperation(@Qualifier("classified") final StartEventOperation startEventOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                         final AccessControlService accessControlService,
                                         final UIDService uidService,
                                         @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                         CaseAccessService caseAccessService) {

        this.startEventOperation = startEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.accessControlService = accessControlService;
        this.uidService = uidService;
        this.caseAccessService = caseAccessService;
        this.draftGateway = draftGateway;
    }

    @Override
    public StartEventResult triggerStartForCaseType(String caseTypeId, String eventId, Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                        eventId,
                                                                                        ignoreWarning));
    }

    @Override
    public StartEventResult triggerStartForCase(String caseReference, String eventId, Boolean ignoreWarning) {

        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return caseDetailsRepository.findByReference(caseReference)
            .map(caseDetails -> verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForCase(caseReference,
                                                                                                                      eventId,
                                                                                                                      ignoreWarning)))
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    @Override
    public StartEventResult triggerStartForDraft(String draftReference,
                                                 Boolean ignoreWarning) {

        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForDraft(draftReference,
            ignoreWarning));
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    private Set<String> getCaseRoles(CaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getId() == null || Draft.isDraft(caseDetails.getId())) {
            return caseAccessService.getCaseCreationCaseRoles();
        } else {
            return caseAccessService.getCaseRoles(caseDetails.getId());
        }
    }

    private StartEventResult verifyReadAccess(final String caseTypeId, final StartEventResult startEventResult) {

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);

        Set<String> userRoles = Sets.union(caseAccessService.getUserRoles(), getCaseRoles(startEventResult.getCaseDetails()));

        CaseDetails caseDetails = startEventResult.getCaseDetails();

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            userRoles,
            CAN_READ)) {
            caseDetails.setData(newHashMap());
            caseDetails.setDataClassification(newHashMap());
            return startEventResult;
        }

        if (caseDetails != null) {
            caseDetails.setData(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    userRoles,
                    CAN_READ,
                    false)));
            caseDetails.setDataClassification(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    userRoles,
                    CAN_READ,
                    true)));
        }
        return startEventResult;
    }


}
