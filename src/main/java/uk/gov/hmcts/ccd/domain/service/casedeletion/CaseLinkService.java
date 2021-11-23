package uk.gov.hmcts.ccd.domain.service.casedeletion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseLinkService {

    private final CaseLinkRepository caseLinkRepository;

    @Autowired
    public CaseLinkService(CaseLinkRepository caseLinkRepository) {
        this.caseLinkRepository = caseLinkRepository;
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
}
