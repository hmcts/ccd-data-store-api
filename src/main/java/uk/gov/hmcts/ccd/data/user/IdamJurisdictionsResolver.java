package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import java.util.List;

@Service
@Qualifier(IdamJurisdictionsResolver.QUALIFIER)
@RequestScope
public class IdamJurisdictionsResolver implements JurisdictionsResolver {

    public static final String QUALIFIER = "idam";

    private final UserRepository userRepository;

    @Inject
    public IdamJurisdictionsResolver(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<String> getJurisdictions() {
        return this.userRepository.getCaseworkerUserRolesJurisdictions();
    }
}
