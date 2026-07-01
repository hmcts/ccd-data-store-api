package uk.gov.hmcts.ccd.domain.service.stdapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;

@Named
@Singleton
public class DocumentsOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentsOperation.class);

    private final SecurityUtils securityUtils;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseTypeService caseTypeService;
    private final UIDService uidService;
    private final AccessControlService accessControlService;
    private final CaseAccessService caseAccessService;

    @Inject
    public DocumentsOperation(final SecurityUtils securityUtils,
                              final CaseTypeService caseTypeService,
                              @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                              final CaseDetailsRepository caseDetailsRepository,
                              final AccessControlService accessControlService,
                              final CaseAccessService caseAccessService,
                              final UIDService uidService) {
        this.securityUtils = securityUtils;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseTypeService = caseTypeService;
        this.uidService = uidService;
        this.accessControlService = accessControlService;
        this.caseAccessService = caseAccessService;
    }

    public List<Document> getPrintableDocumentsForCase(final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        CaseDetails caseDetails = getCaseDetails(caseReference);

        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseTypeForJurisdiction(
            caseDetails.getCaseTypeId(),
            caseDetails.getJurisdiction());

        performAuthorizationChecks(caseReference, caseTypeDefinition);

        return retrieveDocuments(caseDetails, caseTypeDefinition);
    }

    private void performAuthorizationChecks(String caseReference, CaseTypeDefinition caseTypeDefinition) {
        Set<AccessProfile> accessProfiles =
            caseAccessService.getAccessProfilesByCaseReference(caseReference);

        if (accessProfiles == null || accessProfiles.isEmpty()) {
            throw new ValidationException("No matching accessProfiles for case");
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition, accessProfiles, CAN_READ)) {
            LOG.error("User lacks READ permission for case type: {}",
                caseTypeDefinition.getId());
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
    }

    private List<Document> retrieveDocuments(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {
        try {
            String documentListUrl = caseTypeDefinition.getPrintableDocumentsUrl();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CaseDetails> requestEntity = new HttpEntity<>(caseDetails, headers);

            Document[] documents = restTemplate.exchange(
                documentListUrl, HttpMethod.POST, requestEntity, Document[].class).getBody();

            if (documents == null) {
                LOG.warn("Document service returned null for case: {}", caseDetails.getReference());
                return Collections.emptyList();
            }

            return Arrays.asList(documents);

        } catch (Exception e) {
            LOG.error("Failed to retrieve documents for case: {}", caseDetails.getReference(), e);
            throw new ServiceException("Unable to retrieve documents.", e);
        }
    }

    private CaseDetails getCaseDetails(String caseReference) {
        return caseDetailsRepository.findByReference(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
    }
}
