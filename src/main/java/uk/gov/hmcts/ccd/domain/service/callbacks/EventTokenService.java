package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.google.common.collect.Maps;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.TextCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.EventTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.RandomKeyGenerator;

import java.util.Date;

@Service
public class EventTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventTokenService.class);

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
                                final CaseEvent event,
                                final Jurisdiction jurisdiction,
                                final CaseType caseType) {

        return generateToken(uid, EMPTY_CASE, event, jurisdiction, caseType);
    }

    public String generateToken(final String uid,
                                final CaseDetails caseDetails,
                                final CaseEvent event,
                                final Jurisdiction jurisdiction,
                                final CaseType caseType) {
        return Jwts.builder()
            .setId(randomKeyGenerator.generate())
            .setSubject(uid)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode(tokenSecret))
            .claim(EventTokenProperties.CASE_ID, caseDetails.getId())
            .claim(EventTokenProperties.TRIGGER_EVENT_ID, event.getId())
            .claim(EventTokenProperties.CASE_TYPE_ID, caseType.getId())
            .claim(EventTokenProperties.JURISDICTION_ID, jurisdiction.getId())
            .claim(EventTokenProperties.CASE_STATE, caseDetails.getState())
            .claim(EventTokenProperties.CASE_VERSION, caseService.hashData(caseDetails))
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
                toString(claims.get(EventTokenProperties.TRIGGER_EVENT_ID)),
                toString(claims.get(EventTokenProperties.CASE_TYPE_ID)),
                toString(claims.get(EventTokenProperties.CASE_VERSION)),
                toString(claims.get(EventTokenProperties.CASE_STATE)));

        } catch (ExpiredJwtException | SignatureException e) {
            throw new EventTokenException(e.getMessage());
        }
    }

    public void validateToken(final String token,
                              final String uid,
                              final CaseEvent event,
                              final Jurisdiction jurisdiction,
                              final CaseType caseType) {
        validateToken(token, uid, EMPTY_CASE, event, jurisdiction, caseType);
    }

    public void validateToken(final String token,
                              final String uid,
                              final CaseDetails caseDetails,
                              final CaseEvent event,
                              final Jurisdiction jurisdiction,
                              final CaseType caseType) {
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("Missing start trigger token");
        }

        try {
            final EventTokenProperties eventTokenProperties = parseToken(token);

            if (!(eventTokenProperties.getEventId() == null || eventTokenProperties.getEventId().equalsIgnoreCase(event.getId())
                && eventTokenProperties.getCaseId() == null || eventTokenProperties.getCaseId().equalsIgnoreCase(caseDetails.getId().toString())
                && eventTokenProperties.getJurisdictionId() == null || eventTokenProperties.getJurisdictionId().equalsIgnoreCase(jurisdiction.getId())
                && eventTokenProperties.getCaseTypeId() == null || eventTokenProperties.getCaseTypeId().equalsIgnoreCase(caseType.getId())
                && eventTokenProperties.getUid() == null || eventTokenProperties.getUid().equalsIgnoreCase(uid))) {
                throw new ResourceNotFoundException("Cannot find matching start trigger");
            }

            if (caseDetails.getState() != null && !caseDetails.getState().equals(eventTokenProperties.getCaseState())) {
                LOGGER.info("Event token validation error: Case state has been altered");
                throw new CaseConcurrencyException("The case state has been altered outside of this transaction");
            }

            final String currentVersion = caseService.hashData(caseDetails);
            if (!currentVersion.equals(eventTokenProperties.getVersion())) {
                LOGGER.info("Event token validation error: Case data has been altered");
                throw new CaseConcurrencyException("The case data has been altered outside of this transaction");
            }
        } catch (EventTokenException e) {
            throw new SecurityException("Token is not valid");
        }
    }

    /**
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
