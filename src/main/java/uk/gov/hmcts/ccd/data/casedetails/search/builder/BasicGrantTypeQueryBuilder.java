package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Slf4j
@Component
public class BasicGrantTypeQueryBuilder extends GrantTypeSqlQueryBuilder {

    @Autowired
    public BasicGrantTypeQueryBuilder(AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl,
                                      ApplicationParams applicationParams) {
        super(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Override
    protected GrantType getGrantType() {
        return GrantType.BASIC;
    }
}
