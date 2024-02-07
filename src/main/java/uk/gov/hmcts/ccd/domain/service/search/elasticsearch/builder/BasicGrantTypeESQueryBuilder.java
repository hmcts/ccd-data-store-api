package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Component
public class BasicGrantTypeESQueryBuilder extends GrantTypeESQueryBuilder {

    BasicGrantTypeESQueryBuilder(AccessControlService accessControlService,
                                 CaseDataAccessControl caseDataAccessControl,
                                 ApplicationParams applicationParams) {
        super(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Override
    protected GrantType getGrantType() {
        return GrantType.BASIC;
    }
}
