package uk.gov.hmcts.ccd.domain.service.callbacks;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.EventTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.RandomKeyGenerator;

import java.util.Date;

import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.TextCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventTokenService {
    private static final CaseDetails EMPTY_CASE = new CaseDetails();

    static {
        EMPTY_CASE.setData(Maps.newHashMap());
    }

    private final RandomKeyGenerator randomKeyGenerator;
    private final String tokenSecret;
    private final CaseService caseService;

    @Autowired
    public EventTokenService(final RandomKeyGenerator randomKeyGenerator,
                             final ApplicationParams applicationParams,
                             final CaseService caseService) {
        this.randomKeyGenerator = randomKeyGenerator;
        this.tokenSecret = applicationParams.getTokenSecret();
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
            .setId(randomKeyGenerator.generate())
            .setSubject(uid)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode(tokenSecret))
            .claim(EventTokenProperties.CASE_ID, caseDetails.getId())
            .claim(EventTokenProperties.EVENT_ID, event.getId())
            .claim(EventTokenProperties.CASE_TYPE_ID, caseTypeDefinition.getId())
            .claim(EventTokenProperties.JURISDICTION_ID, jurisdictionDefinition.getId())
            .claim(EventTokenProperties.CASE_STATE, caseDetails.getState())
            .claim(EventTokenProperties.CASE_VERSION, caseService.hashData(caseDetails))
            .claim(EventTokenProperties.ENTITY_VERSION, caseDetails.getVersion())
            .compact();
    }

    public EventTokenProperties parseToken(final String token) {
        try {
            final Claims claims = Jwts.parser()
                .setSigningKey(TextCodec.BASE64.encode(tokenSecret))
                .parseClaimsJws(token).getBody();

            return new EventTokenProperties(
                claims.getSubject(),
                toString(claims.get(EventTokenProperties.CASE_ID)),
                toString(claims.get(EventTokenProperties.JURISDICTION_ID)),
                toString(claims.get(EventTokenProperties.EVENT_ID)),
                toString(claims.get(EventTokenProperties.CASE_TYPE_ID)),
                toString(claims.get(EventTokenProperties.CASE_VERSION)),
                toString(claims.get(EventTokenProperties.CASE_STATE)),
                toString(claims.get(EventTokenProperties.ENTITY_VERSION)));

        } catch (ExpiredJwtException | SignatureException e) {
            throw new EventTokenException(e.getMessage());
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
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("Missing start trigger token");
        }

        try {
            final EventTokenProperties eventTokenProperties = parseToken(token);

            if (!(eventTokenProperties.getEventId() == null || eventTokenProperties.getEventId().equalsIgnoreCase(event.getId())
                && eventTokenProperties.getCaseId() == null || eventTokenProperties.getCaseId().equalsIgnoreCase(caseDetails.getId().toString())
                && eventTokenProperties.getJurisdictionId() == null || eventTokenProperties.getJurisdictionId().equalsIgnoreCase(jurisdictionDefinition.getId())
                && eventTokenProperties.getCaseTypeId() == null || eventTokenProperties.getCaseTypeId().equalsIgnoreCase(caseTypeDefinition.getId())
                && eventTokenProperties.getUid() == null || eventTokenProperties.getUid().equalsIgnoreCase(uid))) {
                throw new ResourceNotFoundException("Cannot find matching start trigger");
            }

            if (eventTokenProperties.getEntityVersion() != null) {
                caseDetails.setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
            }
        } catch (EventTokenException e) {
            throw new SecurityException("Token is not valid");
        }
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
}
