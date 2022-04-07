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
        final String caseTypeId = caseDetails.getCaseTypeId();
        final List<String> finalCaseLinkReferences =
            caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        List<String> currentCaseLinkReferences =
            getStringCaseReferencesFromCaseLinks(findCaseLinks(caseReference.toString()));
        deleteRemovedCaseLinks(caseReference, currentCaseLinkReferences, finalCaseLinkReferences);
        // TODO: CaseTypeId needs to be 'case type id of the case to which the case links' so will need to possibly
        //  be updated to take in the linked case - this will need to be confirmed
        insertNewCaseLinks(caseReference, caseTypeId, currentCaseLinkReferences, finalCaseLinkReferences);
    }

    private void createCaseLinks(Long caseReference, String caseTypeId, List<String> caseLinks) {
        caseLinks.stream()
            .filter(caseLinkString -> caseLinkString != null && !caseLinkString.isEmpty())
            .forEach(caseLinkReference -> {
                caseLinkRepository.insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(caseReference,
                    Long.parseLong(caseLinkReference),
                    caseTypeId);
                log.debug("inserted case link with id {}, linkedCaseId {} and caseType {}",
                    caseReference, caseLinkReference, caseTypeId);
            });

    }

    private void deleteRemovedCaseLinks(Long caseReference,
                                        List<String> currentCaseLinkReferences,
                                        List<String> finalCaseLinkReferences) {

        final var caseLinksToDelete = currentCaseLinkReferences.stream()
            .distinct()
            .filter(caseLinkString -> caseLinkString != null && !caseLinkString.isEmpty())
            .filter(caseLink -> !finalCaseLinkReferences.contains(caseLink))
            .collect(Collectors.toList());

        caseLinksToDelete.forEach(caseLink -> {
            caseLinkRepository.deleteByCaseReferenceAndLinkedCaseReference(caseReference, Long.parseLong(caseLink));
            log.debug("deleted case link with id {} and linkedCaseId {}", caseReference, caseLink);
        });
    }

    private void insertNewCaseLinks(Long caseReference,
                                    String caseTypeId,
                                    List<String> currentCaseLinkReferences,
                                    List<String> finalCaseLinkReferences) {
        final var caseLinksToInsert = finalCaseLinkReferences.stream()
            .distinct()
            .filter(caseLink -> !currentCaseLinkReferences.contains(caseLink))
            .collect(Collectors.toList());

        createCaseLinks(caseReference, caseTypeId, caseLinksToInsert);
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

    private List<String> getStringCaseReferencesFromCaseLinks(List<CaseLink> caseLinks) {
        return caseLinks
            .stream()
            .map(caseLink -> caseLink.getLinkedCaseReference().toString())
            .collect(Collectors.toList());
    }
}
