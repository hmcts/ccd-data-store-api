package uk.gov.hmcts.ccd.data.persistence;


import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;

@RequiredArgsConstructor
@Service
public class ShellCaseCreator {

    private final DefaultCaseDetailsRepository caseDetailsRepository;
    private final CaseService caseService;

    /**
     * Persists the immutable case pointer in a new, separate transaction.
     * This transaction will commit immediately upon successful completion of this method.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCasePointer(CaseDetails caseDetails) {
        CaseDetails shell = caseService.clone(caseDetails);
        shell.setData(Map.of());
        shell.setSecurityClassification(SecurityClassification.RESTRICTED);
        shell.setLastModified(null);
        shell.setVersion(null);
        shell.setDataClassification(null);
        shell.setState("");
        caseDetailsRepository.set(shell);
    }
}
