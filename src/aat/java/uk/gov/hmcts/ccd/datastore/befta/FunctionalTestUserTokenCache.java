package uk.gov.hmcts.ccd.datastore.befta;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import uk.gov.hmcts.befta.data.UserData;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Reuses authentication without extending a token's lifetime on each cache access. */
final class FunctionalTestUserTokenCache {

    private static final long EXPIRY_MARGIN_SECONDS = 30;
    private final Cache<Key, UserData> users;
    private final Clock clock;

    FunctionalTestUserTokenCache(Clock clock) {
        this(clock, Ticker.systemTicker());
    }

    FunctionalTestUserTokenCache(Clock clock, Ticker ticker) {
        this.clock = clock;
        users = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .ticker(ticker)
            .build();
    }

    synchronized void authenticate(UserData user, String clientId, Authenticator authenticator)
        throws ExecutionException {
        Key key = new Key(user.getUsername(), user.getPassword(), clientId);
        UserData cached = users.getIfPresent(key);
        if (cached != null && isReusable(cached.getAccessToken())) {
            user.setId(cached.getId());
            user.setAccessToken(cached.getAccessToken());
            return;
        }
        users.invalidate(key);
        authenticator.authenticate(user, clientId);
        if (isReusable(user.getAccessToken())) {
            users.put(key, new UserData(user));
        }
    }

    private boolean isReusable(String token) {
        if (token == null) {
            return false;
        }
        try {
            // Decoding only determines cache lifetime; the API still verifies the token.
            Date expiry = JWT.decode(token).getExpiresAt();
            Instant refreshBefore = clock.instant().plusSeconds(EXPIRY_MARGIN_SECONDS);
            return expiry != null && expiry.toInstant().isAfter(refreshBefore);
        } catch (JWTDecodeException exception) {
            return false;
        }
    }

    private record Key(String username, String password, String clientId) { }

    @FunctionalInterface
    interface Authenticator {
        void authenticate(UserData user, String clientId) throws ExecutionException;
    }
}
