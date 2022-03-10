package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Component
public class ChallengedGrantTypeESQueryBuilder extends GrantTypeESQueryBuilder {

    ChallengedGrantTypeESQueryBuilder(AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl) {
        super(accessControlService, caseDataAccessControl);
    }

    @Override
    protected GrantType getGrantType() {
        return GrantType.CHALLENGED;
    }
}
