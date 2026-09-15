package uk.gov.hmcts.ccd.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
public class MultiIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> validIssuers;

    private final OAuth2Error errorIssuerInvalid =
        new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The issuer is missing or invalid", null);

    public MultiIssuerValidator(List<String> validIssuers) {
        Assert.notEmpty(validIssuers, "Valid issuers list should not be null or empty");
        Assert.noNullElements(validIssuers, "Valid issuers list should not contain any null elements");
        log.info("Valid issuers list: {}", validIssuers);
        this.validIssuers = validIssuers;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getClaimAsString(JwtClaimNames.ISS) == null ? "" : jwt.getClaimAsString(JwtClaimNames.ISS);

        if (!issuer.isEmpty() && validIssuers.contains(issuer)) {
            log.debug("Valid issuer: [{}]", issuer);
            return OAuth2TokenValidatorResult.success();
        } else {
            log.error("Invalid issuer: [{}]", issuer);
            return OAuth2TokenValidatorResult.failure(errorIssuerInvalid);
        }
    }
}