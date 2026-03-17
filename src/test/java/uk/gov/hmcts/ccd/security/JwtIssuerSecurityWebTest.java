package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.ccd.util.KeyGenerator;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web-security slice coverage for request rejection on issuer mismatch without full app dependencies.
@SpringBootTest(classes = JwtIssuerSecurityWebTest.TestApplication.class)
@AutoConfigureMockMvc
class JwtIssuerSecurityWebTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectRequestWhenJwtIssuerDoesNotMatchConfiguredIssuer() throws Exception {
        mockMvc.perform(get("/test/jwt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedJwt(INVALID_ISSUER)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowRequestWhenJwtIssuerMatchesConfiguredIssuer() throws Exception {
        mockMvc.perform(get("/test/jwt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedJwt(VALID_ISSUER)))
            .andExpect(status().isOk());
    }

    private String signedJwt(String issuer) throws Exception {
        Instant now = Instant.now();
        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(KeyGenerator.getRsaJWK().getKeyID())
                .build(),
            new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject("123")
                .issueTime(Date.from(now.minusSeconds(60)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build()
        );
        signedJwt.sign(new RSASSASigner(KeyGenerator.getRsaJWK().toPrivateKey()));
        return signedJwt.serialize();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
    })
    @Import({TestSecurityConfiguration.class, TestController.class})
    static class TestApplication {
    }

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }));
            return http.build();
        }

        @Bean
        JwtDecoder jwtDecoder() throws JOSEException {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey)KeyGenerator.getRsaJWK()
                .toPublicKey()).build();
            OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(VALID_ISSUER)
            );
            jwtDecoder.setJwtValidator(validator);
            return jwtDecoder;
        }
    }

    @Controller
    static class TestController {
        @GetMapping("/test/jwt")
        @ResponseBody
        ResponseEntity<String> jwtProtectedEndpoint() {
            return ResponseEntity.ok("ok");
        }
    }
}
