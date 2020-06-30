package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator.UserRoleValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseRoleAccessException;
import uk.gov.hmcts.ccd.v2.V2;

@Service
@Qualifier("authorised")
public class AuthorisedSupplementaryDataOperation implements SupplementaryDataOperation {

    private SupplementaryDataOperation supplementaryDataOperation;

    private UserRoleValidator roleValidator;

    private final CaseDetailsRepository caseDetailsRepository;

    @Autowired
    public AuthorisedSupplementaryDataOperation(final @Qualifier("default") SupplementaryDataOperation supplementaryDataOperation,
                                                final @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository,
                                                final @Qualifier("default") UserRoleValidator roleValidator) {
        this.supplementaryDataOperation = supplementaryDataOperation;
        this.roleValidator = roleValidator;
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        Optional<CaseDetails> caseDetails = this.caseDetailsRepository.findByReference(caseReference);
        if (caseDetails.isPresent()) {
            if (this.roleValidator.canUpdateSupplementaryData(caseDetails.get())) {
                return this.supplementaryDataOperation.updateSupplementaryData(caseReference, supplementaryData);
            }
            throw new CaseRoleAccessException(V2.Error.NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA);
        }
        throw new CaseNotFoundException(V2.Error.CASE_NOT_FOUND);
    }
}
