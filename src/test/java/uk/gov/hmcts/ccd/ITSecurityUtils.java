package uk.gov.hmcts.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

@Service
@Slf4j
@Primary
public class ITSecurityUtils extends SecurityUtils {
    private static final String USER_JWT = "8gf364fg367f67";
    private static final String USER_ROLES = "I-TEST";

    public ITSecurityUtils(AuthTokenGenerator authTokenGenerator, IdamRepository idamRepository) {
        super(authTokenGenerator, idamRepository);
    }

    @Override
    public String getUserToken() {
        return USER_JWT;
    }

    @Override
    public String getUserRolesHeader() {
        return USER_ROLES;
    }
}
