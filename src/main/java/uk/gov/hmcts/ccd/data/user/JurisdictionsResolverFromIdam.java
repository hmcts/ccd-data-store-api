package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Qualifier(JurisdictionsResolverFromIdam.QUALIFIER)
@RequestScope
public class JurisdictionsResolverFromIdam implements JurisdictionsResolver {

    public static final String QUALIFIER = "JurisdictionsResolverFromIdam";

    @Override
    public List<String> getJurisdictionsFromIdam(String[] roles) {

        return Arrays.stream(roles).map(role ->  role.split("-"))
                .filter(array -> array.length >= 2)
            .map(element -> element[1])
            .distinct()
            .collect(Collectors.toList());

    }
}
