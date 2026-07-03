package uk.gov.hmcts.ccd.domain.service.callbacks;

import io.jsonwebtoken.JwtException;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.EventTokenException;
import uk.gov.hmcts.ccd.infrastructure.RandomKeyGenerator;

import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventTokenService {
    private static final CaseDetails EMPTY_CASE = new CaseDetails();

    static {
        EMPTY_CASE.setData(Maps.newHashMap());
    }

    private final RandomKeyGenerator randomKeyGenerator;
    private final SecretKey secretKey;
    private final CaseService caseService;
    private final boolean isValidateTokenClaims;


    @Autowired
    public EventTokenService(final RandomKeyGenerator randomKeyGenerator,
                             final ApplicationParams applicationParams,
                             final CaseService caseService) {
        this.randomKeyGenerator = randomKeyGenerator;
        byte[] keyBytes = Decoders.BASE64.decode(applicationParams.getTokenSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.isValidateTokenClaims = applicationParams.isValidateTokenClaims();
        this.caseService = caseService;
    }

    public String generateToken(final String uid,
                                final CaseEventDefinition event,
                                final JurisdictionDefinition jurisdictionDefinition,
                                final CaseTypeDefinition caseTypeDefinition) {

        return generateToken(uid, EMPTY_CASE, event, jurisdictionDefinition, caseTypeDefinition);
    }

    public String generateToken(final String uid,
                                final CaseDetails caseDetails,
                                final CaseEventDefinition event,
                                final JurisdictionDefinition jurisdictionDefinition,
                                final CaseTypeDefinition caseTypeDefinition) {
        return Jwts.builder()
            .id(randomKeyGenerator.generate())
            .subject(uid)
            .issuedAt(new Date())
            .signWith(secretKey)
            .claim(EventTokenProperties.CASE_ID, caseDetails.getId())
            .claim(EventTokenProperties.EVENT_ID, event.getId())
            .claim(EventTokenProperties.CASE_TYPE_ID, caseTypeDefinition.getId())
            .claim(EventTokenProperties.JURISDICTION_ID, jurisdictionDefinition.getId())
            .claim(EventTokenProperties.CASE_STATE, caseDetails.getState())
            .claim(EventTokenProperties.CASE_VERSION, caseService.hashData(caseDetails))
            .claim(EventTokenProperties.ENTITY_VERSION, caseDetails.getVersion())
            .claim(EventTokenProperties.CASE_REVISION, caseDetails.getRevision())
            .compact();
    }

    public EventTokenProperties parseToken(final String token) {
        try {
            final Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return new EventTokenProperties(
                claims.getSubject(),
                toString(claims.get(EventTokenProperties.CASE_ID)),
                toString(claims.get(EventTokenProperties.JURISDICTION_ID)),
                toString(claims.get(EventTokenProperties.EVENT_ID)),
                toString(claims.get(EventTokenProperties.CASE_TYPE_ID)),
                toString(claims.get(EventTokenProperties.CASE_VERSION)),
                toString(claims.get(EventTokenProperties.CASE_STATE)),
                toString(claims.get(EventTokenProperties.ENTITY_VERSION)),
                toString(claims.get(EventTokenProperties.CASE_REVISION)));

        } catch (JwtException e) {
            throw new EventTokenException("Token is not valid: " + e.getMessage());
        }
    }

    public void validateToken(final String token,
                              final String uid,
                              final CaseEventDefinition event,
                              final JurisdictionDefinition jurisdictionDefinition,
                              final CaseTypeDefinition caseTypeDefinition) {
        validateToken(token, uid, EMPTY_CASE, event, jurisdictionDefinition, caseTypeDefinition);
    }

    public void validateToken(final String token,
                              final String uid,
                              final CaseDetails caseDetails,
                              final CaseEventDefinition event,
                              final JurisdictionDefinition jurisdictionDefinition,
                              final CaseTypeDefinition caseTypeDefinition) {
        validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition, false);
    }

    public void validateToken(final String token,
                              final String uid,
                              final CaseDetails caseDetails,
                              final CaseEventDefinition event,
                              final JurisdictionDefinition jurisdictionDefinition,
                              final CaseTypeDefinition caseTypeDefinition,
                              final boolean revisionRequired) {
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("Missing start trigger token");
        }

        final EventTokenProperties eventTokenProperties = parseToken(token);

        if (isValidateTokenClaims && !isTokenPropertiesMatching(eventTokenProperties, uid, caseDetails, event,
            jurisdictionDefinition,
            caseTypeDefinition)) {
            throw new EventTokenException("Token properties do not match the expected values");
        }

        if (eventTokenProperties.getEntityVersion() != null) {
            caseDetails.setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
        }
        applyRevision(eventTokenProperties.getCaseRevision(), caseDetails, revisionRequired);
    }

    private boolean isTokenPropertiesMatching(EventTokenProperties eventTokenProperties,
                                              String uid,
                                              CaseDetails caseDetails,
                                              CaseEventDefinition event,
                                              JurisdictionDefinition jurisdictionDefinition,
                                              CaseTypeDefinition caseTypeDefinition) {
        return isMatching(eventTokenProperties.getEventId(), event.getId())
            && isMatching(eventTokenProperties.getCaseId(), caseDetails.getId())
            && isMatching(eventTokenProperties.getJurisdictionId(), jurisdictionDefinition.getId())
            && isMatching(eventTokenProperties.getCaseTypeId(), caseTypeDefinition.getId())
            && isMatching(eventTokenProperties.getUid(), uid);
    }

    private boolean isMatching(String tokenValue, String actualValue) {
        return Optional.ofNullable(tokenValue)
            .map(value -> value.equalsIgnoreCase(actualValue))
            .orElse(true);
    }

    /**
     * Convert to string.
     *
     * @param object Object to convert to string
     * @return <code>object.toString()</code> when object is not null; <code>null</code> otherwise
     */
    private String toString(Object object) {
        if (null == object) {
            return null;
        }

        return object.toString();
    }

    private void applyRevision(String revisionClaim,
                               CaseDetails caseDetails,
                               boolean revisionRequired) {
        if (revisionClaim != null) {
            caseDetails.setRevision(Long.parseLong(revisionClaim));
        } else if (revisionRequired) {
            // Old start-event tokens (minted before we added the revision claim) cannot safely
            // participate in decentralised optimistic locking, so ask the caller to restart.
            throw new BadRequestException("Start trigger token has expired. Please restart the event.");
        }
    }
}
