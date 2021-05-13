package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.EndpointAuthorisationService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseAccessException;
import uk.gov.hmcts.ccd.v2.V2;

@Service
@Qualifier("authorised")
public class AuthorisedSupplementaryDataUpdateOperation implements SupplementaryDataUpdateOperation {

    private SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;

    private EndpointAuthorisationService authorisationService;

    private final CaseDetailsRepository caseDetailsRepository;

    private static final Logger LOG = LoggerFactory.getLogger(AuthorisedSupplementaryDataUpdateOperation.class);

    @Autowired
    public AuthorisedSupplementaryDataUpdateOperation(final @Qualifier("default") SupplementaryDataUpdateOperation
                                                              supplementaryDataUpdateOperation,
                                                      final @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                                          CaseDetailsRepository caseDetailsRepository,
                                                      final @Qualifier("default")
                                                              EndpointAuthorisationService authorisationService) {
        this.supplementaryDataUpdateOperation = supplementaryDataUpdateOperation;
        this.authorisationService = authorisationService;
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataUpdateRequest
        supplementaryData) {
        LOG.error(String.format("Update supplementary data case reference => %s", caseReference));
        Optional<CaseDetails> caseDetails = this.caseDetailsRepository.findByReference(caseReference);

        if (caseDetails.isPresent()) {
            LOG.error(String.format("case details present for case reference => %s", caseReference));
            if (this.authorisationService.isAccessAllowed(caseDetails.get())) {
                LOG.error(String.format("Authorised to Update supplementary data case reference => %s", caseReference));
                return this.supplementaryDataUpdateOperation.updateSupplementaryData(caseReference, supplementaryData);
            }
            throw new CaseAccessException(V2.Error.NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA);
        }
        throw new CaseNotFoundException(V2.Error.CASE_NOT_FOUND);
    }
}
