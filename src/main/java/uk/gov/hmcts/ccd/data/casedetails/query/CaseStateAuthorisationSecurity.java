package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.service.common.AuthorisedCaseDefinitionDataService;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Component
public class CaseStateAuthorisationSecurity implements CaseDetailsAuthorisationSecurity {

    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @Autowired
    public CaseStateAuthorisationSecurity(AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService) {
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
    }

    @Override
    public <T> void secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        List<String> caseStateIds = authorisedCaseDefinitionDataService
            .getUserAuthorisedCaseStates(metadata.getJurisdiction(), metadata.getCaseTypeId(), CAN_READ)
            .stream()
            .map(CaseState::getId)
            .collect(Collectors.toList());

        builder.whereStates(caseStateIds);
    }

}
