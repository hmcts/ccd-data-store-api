package uk.gov.hmcts.ccd.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

@Component
public class JwtAuthorityExtractor extends JwtAuthenticationConverter {

    public static final String TOKEN_NAME = "tokenName";

    private final IdamRepository idamRepository;

    @Autowired
    public JwtAuthorityExtractor(IdamRepository idamRepository) {
        this.idamRepository = idamRepository;
    }

    @Override
    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (jwt.containsClaim(TOKEN_NAME)) {
            if (jwt.getClaim(TOKEN_NAME).equals(ACCESS_TOKEN)) {
                UserInfo userInfo = idamRepository.getUserInfo(jwt.getTokenValue());
                authorities = extractAuthorityFromClaims(userInfo.getRoles());
            } else if (jwt.getClaim(TOKEN_NAME).equals(ID_TOKEN)) {
                authorities = extractAuthorityFromClaims(((List<String>) jwt.getClaims().get("roles")));
            }

        }
        return authorities;
    }

    private List<GrantedAuthority> extractAuthorityFromClaims(List<String> roles) {
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

}
