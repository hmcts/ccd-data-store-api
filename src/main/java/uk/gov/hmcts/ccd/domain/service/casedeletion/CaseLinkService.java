package uk.gov.hmcts.ccd.domain.service.casedeletion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseLinkService {

    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseLinkRepository caseLinkRepository;
    private final CaseLinkMapper caseLinkMapper;

    @Autowired
    public CaseLinkService(CaseLinkRepository caseLinkRepository,
                           CaseLinkMapper caseLinkMapper,
                           @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                   CaseDetailsRepository caseDetailsRepository) {
        this.caseLinkRepository = caseLinkRepository;
        this.caseLinkMapper = caseLinkMapper;
        this.caseDetailsRepository = caseDetailsRepository;
    }

    public void updateCaseLinks(Long caseId,
                                String caseTypeId,
                                List<String> preCallbackCaseLinks,
                                List<String> postCallbackCaseLinks) {
        deleteRemovedCaseLinks(caseId, preCallbackCaseLinks, postCallbackCaseLinks);
        insertNewCaseLinks(caseId, caseTypeId, preCallbackCaseLinks, postCallbackCaseLinks);
    }

    public void createCaseLinks(Long caseReference, String caseTypeId, List<String> caseLinks) {
        caseLinks.stream()
            .filter(caseLinkString -> caseLinkString != null && !caseLinkString.isEmpty())
            .forEach(caseLink -> {
                caseLinkRepository.insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(caseReference,
                        Long.parseLong(caseLink),
                        caseTypeId);
                log.debug("inserted case link with id {}, linkedCaseId {} and caseType {}",
                    caseReference, caseLink, caseTypeId);
            });

    }

    private void deleteRemovedCaseLinks(Long caseId,
                                                List<String> preCallbackData,
                                                List<String> postCallbackData) {

        final var caseLinksToDelete = preCallbackData.stream()
            .distinct()
            .filter(caseLinkString -> caseLinkString != null && !caseLinkString.isEmpty())
            .filter(caseLink -> !postCallbackData.contains(caseLink))
            .collect(Collectors.toList());

        caseLinksToDelete.forEach(caseLink -> {
            caseLinkRepository.deleteByCaseReferenceAndLinkedCaseReference(caseId, Long.parseLong(caseLink));
            log.debug("deleted case link with id {} and linkedCaseId {}", caseId, caseLink);
        });
    }

    private void insertNewCaseLinks(Long caseId,
                                    String caseTypeId,
                                    List<String> preCallbackData,
                                    List<String> postCallbackData) {
        final var caseLinksToInsert = postCallbackData.stream()
            .distinct()
            .filter(caseLink -> !preCallbackData.contains(caseLink))
            .collect(Collectors.toList());

        createCaseLinks(caseId, caseTypeId, caseLinksToInsert);
    }

    public List<CaseLink> findCaseLinks(String caseReference) {
        List<CaseLinkEntity> allByCaseReference =
            caseLinkRepository.findAllByCaseReference(Long.parseLong(caseReference));
        List<CaseLink> allLinkedCases =
            caseLinkMapper.entitiesToModels(allByCaseReference);
        List<Long> linkedCaseReferences =
            caseDetailsRepository.findCaseReferencesByIds(getAllLinkedCaseIds(allByCaseReference));

        updateCaseLinkObjectIdsToReferences(allLinkedCases, linkedCaseReferences, Long.valueOf(caseReference));

        return allLinkedCases;
    }

    private List<Long> getAllLinkedCaseIds(List<CaseLinkEntity> caseLinkEntities) {
        return caseLinkEntities
            .stream()
            .map(c -> c.getCaseLinkPrimaryKey().getLinkedCaseId())
            .collect(Collectors.toList());
    }

    private void updateCaseLinkObjectIdsToReferences(List<CaseLink> allLinkedCases,
                                                     List<Long> linkedCaseReferences,
                                                     Long caseReference) {
        for (int i = 0; i < allLinkedCases.size(); i++) {
            allLinkedCases.get(i).setCaseId(caseReference);
            allLinkedCases.get(i).setLinkedCaseId(linkedCaseReferences.get(i));
        }
    }
}
