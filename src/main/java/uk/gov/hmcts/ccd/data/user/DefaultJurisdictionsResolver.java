package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Qualifier(DefaultJurisdictionsResolver.QUALIFIER)
public class DefaultJurisdictionsResolver implements JurisdictionsResolver {
    public static final String QUALIFIER = "default";

    private final ApplicationParams applicationParams;
    private final JurisdictionsResolver idamJurisdictionsResolver;
    private final JurisdictionsResolver attributeBasedJurisdictionsResolver;

    public DefaultJurisdictionsResolver(ApplicationParams applicationParams,
                                        @Qualifier(IdamJurisdictionsResolver.QUALIFIER)
                                            JurisdictionsResolver idamJurisdictionsResolver,
                                        @Qualifier(AttributeBasedJurisdictionsResolver.QUALIFIER)
                                            JurisdictionsResolver attributeBasedJurisdictionsResolver) {
        this.idamJurisdictionsResolver = idamJurisdictionsResolver;
        this.attributeBasedJurisdictionsResolver = attributeBasedJurisdictionsResolver;
        this.applicationParams = applicationParams;
    }

    @Override
    public List<String> getJurisdictions() {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            List<String> jurisdictions = attributeBasedJurisdictionsResolver.getJurisdictions();
            List<String> roleJurisdictions = idamJurisdictionsResolver.getJurisdictions();

            return Stream.concat(jurisdictions.stream(), roleJurisdictions.stream())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        } else {
            return idamJurisdictionsResolver.getJurisdictions();
        }
    }

}
