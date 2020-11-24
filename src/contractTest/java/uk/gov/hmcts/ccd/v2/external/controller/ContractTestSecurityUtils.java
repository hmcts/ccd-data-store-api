package uk.gov.hmcts.ccd.v2.external.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamApi;

@Service
@Slf4j
@Primary
public class ContractTestSecurityUtils extends SecurityUtils {

    @Autowired
    public ContractTestSecurityUtils(AuthTokenGenerator authTokenGenerator,
                                     IdamRepository idamRepository, IdamApi idamApi) {
        super(authTokenGenerator, idamRepository, idamApi);
    }

    @Override
    public String getUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public String getUserToken() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }
}
