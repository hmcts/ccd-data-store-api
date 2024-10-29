package uk.gov.hmcts.ccd.domain.service.getcase;

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

@Service
@Qualifier("default")
public class DefaultGetCaseOperation implements GetCaseOperation {
    private final CaseDetailsRepository caseDetailsRepository;
    private final UIDService uidService;
    private final ApplicationParams applicationParams;
    private final PocApiClient pocApiClient;
    private final SecurityUtils securityUtils;

    @Autowired
    public DefaultGetCaseOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                       final CaseDetailsRepository caseDetailsRepository,
                                   final UIDService uidService,
                                   final ApplicationParams applicationParams,
                                   final PocApiClient pocApiClient,
                                   final SecurityUtils securityUtils) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
        this.applicationParams = applicationParams;
        this.pocApiClient = pocApiClient;
        this.securityUtils = securityUtils;
    }

    @Override
    public Optional<CaseDetails> execute(final String jurisdictionId,
                                         final String caseTypeId,
                                         final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        if (this.applicationParams.getPocCaseTypes().contains(caseTypeId)) {
            return Optional.ofNullable(pocApiClient.getCase(caseReference, securityUtils.getUserBearerToken(),
                    securityUtils.getServiceAuthorization()));
        }

        return Optional.ofNullable(caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference));
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

//        return Optional.ofNullable(caseDetailsRepository.findByReference(Long.valueOf(caseReference)));
        securityUtils.getServiceAuthorization();
        return Optional.ofNullable(pocApiClient.getCase(caseReference, securityUtils.getUserBearerToken(),
                securityUtils.getServiceAuthorization()));
    }
}
