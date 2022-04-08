package uk.gov.hmcts.ccd.domain.service.casedeletion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseLinkService {

    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseLinkRepository caseLinkRepository;
    private final CaseLinkMapper caseLinkMapper;
    private final CaseLinkExtractor caseLinkExtractor;

    @Inject
    public CaseLinkService(CaseLinkRepository caseLinkRepository,
                           CaseLinkMapper caseLinkMapper,
                           @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                               CaseDetailsRepository caseDetailsRepository,
                           CaseLinkExtractor caseLinkExtractor) {
        this.caseLinkRepository = caseLinkRepository;
        this.caseLinkMapper = caseLinkMapper;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseLinkExtractor = caseLinkExtractor;
    }

    @Transactional
    public void updateCaseLinks(CaseDetails caseDetails, List<CaseFieldDefinition> caseFieldDefinitions) {
        final Long caseReference = caseDetails.getReference();
        final List<CaseLink> caseLinksWithReferences =
            caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // NB: delete all and re-add as this will update any links that have a changed StandardFlag value
        caseLinkRepository.deleteAllByCaseReference(caseReference);
        // TODO: CaseTypeId needs to be 'case type id of the case to which the case links' so will need to possibly
        //  be updated to take in the linked case - this will need to be confirmed
        createCaseLinks(caseReference, caseDetails.getCaseTypeId(), caseLinksWithReferences);
    }

    private void createCaseLinks(Long caseReference, String caseTypeId, List<CaseLink> caseLinksWithReferences) {
        caseLinksWithReferences.stream()
            .filter(caseLink -> caseLink != null && caseLink.getLinkedCaseReference() != null)
            .forEach(caseLinkReference -> {
                caseLinkRepository.insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(caseReference,
                    caseLinkReference.getLinkedCaseReference(),
                    caseTypeId,
                    caseLinkReference.getStandardLink());
                log.debug("inserted case link with id {}, linkedCaseId {} and caseType {}",
                    caseReference, caseLinkReference.getLinkedCaseReference(), caseTypeId);
            });
    }

    public List<CaseLink> findCaseLinks(String caseReference) {
        List<CaseLinkEntity> allByCaseReference =
            caseLinkRepository.findAllByCaseReference(Long.parseLong(caseReference));
        List<CaseLink> allLinkedCases =
            caseLinkMapper.entitiesToModels(allByCaseReference);

        return allLinkedCases.stream()
            .map(caseLink -> setCaseLinkReferences(Long.parseLong(caseReference), caseLink))
            .collect(Collectors.toList());
    }

    private CaseLink setCaseLinkReferences(Long caseReference, CaseLink caseLink) {

        caseLink.setCaseReference(caseReference);
        caseDetailsRepository.findById(null, caseLink.getLinkedCaseId())
            .ifPresent(caseDetails -> caseLink.setLinkedCaseReference(caseDetails.getReference()));

        return caseLink;
    }

}
