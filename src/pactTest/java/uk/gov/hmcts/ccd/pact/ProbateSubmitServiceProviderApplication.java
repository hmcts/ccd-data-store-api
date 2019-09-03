package uk.gov.hmcts.ccd.pact;

import java.util.Set;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.AuthorisedGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

@ComponentScan(basePackages = {"uk.gov.hmcts.ccd.v2.external.controller", "uk.gov.hmcts.ccd.domain.service.getcase",
    "uk.gov.hmcts.ccd.domain.service.common","uk.gov.hmcts.ccd.data.user", "uk.gov.hmcts.ccd.data.definition",
"uk.gov.hmcts.ccd.data.caseaccess"}
)
public class ProbateSubmitServiceProviderApplication {


}
