package uk.gov.hmcts.ccd.domain.service.callbacks;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.infrastructure.RandomKeyGenerator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pins the HMAC key derivation of {@link EventTokenService} across the jjwt 0.9 -&gt; 0.12 API migration.
 *
 * <p>The migration replaced {@code signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode(tokenSecret))} —
 * whose {@code String} overload Base64-<em>decodes</em> its argument, making the effective key
 * {@code tokenSecret.getBytes()} — with the modern {@code signWith(SecretKey, MacAlgorithm)}. It must stay a pure
 * API migration: same key bytes, same algorithm, same resulting signature.
 *
 * <p>An intermediate version of this branch derived the key as {@code Decoders.BASE64.decode(tokenSecret)} instead,
 * which is a different key. That would have invalidated every in-flight event token on deploy, and — because
 * {@code Keys.hmacShaKeyFor} rejects keys under 256 bits — would have failed at bean creation for any deployed
 * secret shorter than 43 Base64 characters. The real {@code DATA_STORE_TOKEN_SECRET} comes from an Azure Key Vault
 * and cannot be asserted here, so these tests pin the properties that make its value irrelevant.
 */
class EventTokenServiceKeyDerivationTest {

    /**
     * The pre-branch default for {@code ccd.token.secret}; also the shortest secret shape seen in configuration.
     */
    private static final String SHORT_40_CHAR_SECRET = "A".repeat(40);

    /**
     * The current default for {@code ccd.token.secret}.
     */
    private static final String CURRENT_60_CHAR_SECRET = "A".repeat(60);

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private RandomKeyGenerator randomKeyGenerator;

    @Mock
    private CaseService caseService;

    private AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        lenient().when(randomKeyGenerator.generate()).thenReturn("token-id");
        lenient().when(caseService.hashData(any())).thenReturn("case-data-hash");
        lenient().when(applicationParams.isValidateTokenClaims()).thenReturn(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    private EventTokenService serviceWithSecret(final String secret) {
        when(applicationParams.getTokenSecret()).thenReturn(secret);
        return new EventTokenService(randomKeyGenerator, applicationParams, caseService);
    }

    private static CaseDetails caseDetails() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId("1234567890123456");
        caseDetails.setState("CaseCreated");
        caseDetails.setVersion(3);
        caseDetails.setRevision(7L);
        caseDetails.setData(new HashMap<>());
        return caseDetails;
    }

    private static CaseEventDefinition event() {
        final CaseEventDefinition event = new CaseEventDefinition();
        event.setId("eventId");
        return event;
    }

    private static JurisdictionDefinition jurisdiction() {
        final JurisdictionDefinition jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId("jurisdictionId");
        return jurisdiction;
    }

    /**
     * The key the pre-migration code effectively signed and verified with.
     *
     * <p>Old signing was {@code signWith(HS256, TextCodec.BASE64.encode(secret))}. That deprecated overload treats
     * its argument as a Base64-encoded key and decodes it, so the chain below reproduces the exact same bytes
     * without calling any deprecated jjwt API. Old verification was
     * {@code Keys.hmacShaKeyFor(tokenSecret.getBytes())} — the same bytes again.
     */
    private static byte[] legacyEffectiveKeyBytes(final String secret) {
        final String base64EncodedSecret = Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
        return Decoders.BASE64.decode(base64EncodedSecret);
    }

    private static CaseTypeDefinition caseType() {
        final CaseTypeDefinition caseType = new CaseTypeDefinition();
        caseType.setId("caseTypeId");
        return caseType;
    }

    /**
     * Mints a token the way the pre-migration code did: HS256 over the raw secret bytes.
     */
    private static String mintTokenWithLegacyScheme(final String secret) {
        return Jwts.builder()
            .id("token-id")
            .subject("userId")
            .issuedAt(new Date())
            .signWith(new SecretKeySpec(legacyEffectiveKeyBytes(secret), "HmacSHA256"), Jwts.SIG.HS256)
            .claim(EventTokenProperties.CASE_ID, "1234567890123456")
            .claim(EventTokenProperties.EVENT_ID, "eventId")
            .claim(EventTokenProperties.CASE_TYPE_ID, "caseTypeId")
            .claim(EventTokenProperties.JURISDICTION_ID, "jurisdictionId")
            .claim(EventTokenProperties.CASE_STATE, "CaseCreated")
            .claim(EventTokenProperties.CASE_VERSION, "case-data-hash")
            .claim(EventTokenProperties.ENTITY_VERSION, 3)
            .claim(EventTokenProperties.CASE_REVISION, 7L)
            .compact();
    }

    /**
     * The {@code alg} value from a compact JWT's (Base64URL-encoded) header.
     */
    private static String headerAlgorithm(final String jwt) {
        final String header = new String(Base64.getUrlDecoder().decode(jwt.substring(0, jwt.indexOf('.'))),
            StandardCharsets.UTF_8);
        assertThat(header).contains("\"alg\"");
        return header.replaceAll(".*\"alg\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    /**
     * A valid Base64 string that decodes to exactly {@code byteCount} bytes.
     */
    private static String base64SecretOfDecodedLength(final int byteCount) {
        final byte[] raw = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            raw[i] = (byte) (i + 1);
        }
        return Base64.getEncoder().encodeToString(raw);
    }

    @Nested
    @DisplayName("the migration preserves the effective HMAC key")
    class KeyDerivation {

        @Test
        @DisplayName("the effective key is still the raw UTF-8 bytes of the secret")
        void effectiveKeyIsRawSecretBytes() {
            assertThat(legacyEffectiveKeyBytes(CURRENT_60_CHAR_SECRET))
                .as("the deprecated signWith(alg, String) overload Base64-decoded back to the raw secret bytes")
                .isEqualTo(CURRENT_60_CHAR_SECRET.getBytes(StandardCharsets.UTF_8));

            assertThat(Decoders.BASE64.decode(CURRENT_60_CHAR_SECRET))
                .as("Base64-decoding the secret itself would have produced a different, 45-byte key")
                .hasSize(45)
                .isNotEqualTo(CURRENT_60_CHAR_SECRET.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("a token minted before the migration is still accepted afterwards")
        void inFlightTokenMintedBeforeDeployStillVerifies() {
            final String tokenMintedByOldPod = mintTokenWithLegacyScheme(CURRENT_60_CHAR_SECRET);

            final EventTokenProperties properties =
                serviceWithSecret(CURRENT_60_CHAR_SECRET).parseToken(tokenMintedByOldPod);

            assertThat(properties.getUid()).isEqualTo("userId");
            assertThat(properties.getEventId()).isEqualTo("eventId");
            assertThat(properties.getCaseId()).isEqualTo("1234567890123456");
        }

        @Test
        @DisplayName("a token minted after the migration is still accepted by a pre-migration pod")
        void newTokenIsAcceptedByOldPodDuringRollingDeploy() {
            final String tokenMintedByNewPod = serviceWithSecret(CURRENT_60_CHAR_SECRET)
                .generateToken("userId", caseDetails(), event(), jurisdiction(), caseType());

            // The pre-migration verification key: Keys.hmacShaKeyFor(tokenSecret.getBytes()).
            final SecretKey oldPodKey = new SecretKeySpec(
                CURRENT_60_CHAR_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            assertThatCode(() -> Jwts.parser()
                .verifyWith(oldPodKey)
                .build()
                .parseSignedClaims(tokenMintedByNewPod))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("generate and parse round-trip against the real signing key")
        void roundTripsWithItself() {
            final EventTokenService service = serviceWithSecret(CURRENT_60_CHAR_SECRET);

            final String token = service.generateToken("userId", caseDetails(), event(), jurisdiction(), caseType());
            final EventTokenProperties properties = service.parseToken(token);

            assertThat(properties.getUid()).isEqualTo("userId");
            assertThat(properties.getCaseId()).isEqualTo("1234567890123456");
            assertThat(properties.getEventId()).isEqualTo("eventId");
            assertThat(properties.getCaseTypeId()).isEqualTo("caseTypeId");
            assertThat(properties.getJurisdictionId()).isEqualTo("jurisdictionId");
            assertThat(properties.getEntityVersion()).isEqualTo("3");
            assertThat(properties.getCaseRevision()).isEqualTo("7");
        }
    }

    @Nested
    @DisplayName("the migration imposes no new constraints on the vault secret")
    class SecretConstraints {

        @Test
        @DisplayName("the pre-branch 40-char secret still works end to end")
        void shortSecretStillWorks() {
            assertThat(Decoders.BASE64.decode(SHORT_40_CHAR_SECRET))
                .as("Base64-decoding this secret yields 30 bytes = 240 bits, which is under the HMAC-SHA floor")
                .hasSize(30);

            final EventTokenService service = serviceWithSecret(SHORT_40_CHAR_SECRET);
            final String token = service.generateToken("userId", caseDetails(), event(), jurisdiction(), caseType());

            assertThat(service.parseToken(token).getUid()).isEqualTo("userId");
        }

        @Test
        @DisplayName("the secret does not have to be valid Base64")
        void nonBase64SecretWorks() {
            final String vaultStyleSecret = "s0me!Vault@Secret#With$Punctuation%And^Enough&Length*To(Be)Strong";

            final EventTokenService service = serviceWithSecret(vaultStyleSecret);
            final String token = service.generateToken("userId", caseDetails(), event(), jurisdiction(), caseType());

            assertThat(service.parseToken(token).getUid()).isEqualTo("userId");
        }

        @Test
        @DisplayName("the bean is created without a Spring context refresh failure")
        void springContextRefreshSucceeds() {
            when(applicationParams.getTokenSecret()).thenReturn(SHORT_40_CHAR_SECRET);

            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                // Registered as ready-made singletons so ApplicationParams' own @Value fields are not processed -
                // only EventTokenService's construction is under test here.
                context.getBeanFactory().registerSingleton("applicationParams", applicationParams);
                context.getBeanFactory().registerSingleton("randomKeyGenerator", randomKeyGenerator);
                context.getBeanFactory().registerSingleton("caseService", caseService);
                context.registerBean(EventTokenService.class);

                assertThatCode(context::refresh).doesNotThrowAnyException();
                assertThat(context.getBean(EventTokenService.class)).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("the signing algorithm stays pinned to HS256")
    class SignatureAlgorithmPinning {

        private String algorithmForSecret(final String secret) {
            return headerAlgorithm(serviceWithSecret(secret)
                .generateToken("userId", caseDetails(), event(), jurisdiction(), caseType()));
        }

        @Test
        @DisplayName("the algorithm does not vary with the deployed secret's length")
        void algorithmIsIndependentOfSecretLength() {
            // Keys.hmacShaKeyFor infers HS256/HS384/HS512 from 32/48/64 bytes of key material, which would make the
            // signing algorithm an implicit function of each environment's secret. SecretKeySpec + an explicit
            // MacAlgorithm removes that coupling.
            assertThat(algorithmForSecret(SHORT_40_CHAR_SECRET)).isEqualTo("HS256");
            assertThat(algorithmForSecret(CURRENT_60_CHAR_SECRET)).isEqualTo("HS256");
            assertThat(algorithmForSecret(base64SecretOfDecodedLength(48))).isEqualTo("HS256");
            assertThat(algorithmForSecret(base64SecretOfDecodedLength(64))).isEqualTo("HS256");
            assertThat(algorithmForSecret("A".repeat(200))).isEqualTo("HS256");
        }
    }
}
