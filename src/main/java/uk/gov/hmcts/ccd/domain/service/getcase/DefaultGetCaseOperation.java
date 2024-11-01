package uk.gov.hmcts.ccd.domain.service.getcase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.Optional;

@Slf4j
@Service
@Qualifier("default")
public class DefaultGetCaseOperation implements GetCaseOperation {
    private final CaseDetailsRepository caseDetailsRepository;
    private final UIDService uidService;
    private final ApplicationParams applicationParams;
    private final PocApiClient pocApiClient;

    @Autowired
    public DefaultGetCaseOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                   final UIDService uidService,
                                   final ApplicationParams applicationParams,
                                   final PocApiClient pocApiClient) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
        this.applicationParams = applicationParams;
        this.pocApiClient = pocApiClient;
    }

    @Override
    public Optional<CaseDetails> execute(final String jurisdictionId,
                                         final String caseTypeId,
                                         final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        if (this.applicationParams.getPocCaseTypes().contains(caseTypeId)) {
            return Optional.ofNullable(pocApiClient.getCase(caseReference));
        }

        return Optional.ofNullable(caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference));
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        CaseDetails caseDetails = pocApiClient.getCase(caseReference);
        if (Optional.ofNullable(caseDetails).isPresent()
                && this.applicationParams.getPocCaseTypes().contains(caseDetails.getCaseTypeId())) {
            log.info("case details id before {}", caseDetails.getId());
            log.info("case details reference before {}", caseDetails.getReference());
            caseDetails.setId(caseDetails.getReference().toString());
            caseDetails.setReference(Long.valueOf(caseReference));
            log.info("case details id  after {}", caseDetails.getId());
            log.info("case details reference after {}", caseDetails.getReference());
            return Optional.of(caseDetails);
        }
        return Optional.ofNullable(caseDetailsRepository.findByReference(Long.valueOf(caseReference)));
    }
}
