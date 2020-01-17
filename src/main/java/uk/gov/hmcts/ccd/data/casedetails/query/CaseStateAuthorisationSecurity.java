package uk.gov.hmcts.ccd.data.casedetails.query;

import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

@Component
public class CaseStateAuthorisationSecurity implements CaseDetailsAuthorisationSecurity {

    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @Autowired
    public CaseStateAuthorisationSecurity(AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService) {
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
    }

    @Override
    public <T> void secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        if (metadata != null) {
            List<String> caseStateIds = authorisedCaseDefinitionDataService
                .getUserAuthorisedCaseStateIds(metadata.getJurisdiction(), metadata.getCaseTypeId(), CAN_READ);

            builder.whereStates(caseStateIds);
        }
    }

}
