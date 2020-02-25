package uk.gov.hmcts.ccd.data.user;

import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Qualifier(IDAMJurisdictionsResolver.QUALIFIER)
@RequestScope
public class IDAMJurisdictionsResolver implements JurisdictionsResolver {

    public static final String QUALIFIER = "default";

    private UserRepository userRepository;

    @Inject
    public IDAMJurisdictionsResolver(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<String> getJurisdictions() {
        String[] roles = this.userRepository.getUserDetails().getRoles();
        return Arrays.stream(roles).map(role ->  role.split("-"))
                .filter(array -> array.length >= 2)
            .map(element -> element[1])
            .distinct()
            .collect(Collectors.toList());
    }
}
