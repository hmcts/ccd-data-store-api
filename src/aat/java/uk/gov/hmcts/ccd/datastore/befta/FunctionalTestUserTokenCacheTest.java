package uk.gov.hmcts.ccd.datastore.befta;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.befta.data.UserData;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FunctionalTestUserTokenCacheTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");
    private final Clock clock = mock(Clock.class);
    private final FunctionalTestUserTokenCache cache = new FunctionalTestUserTokenCache(clock);
    private int authentications;

    @Test
    void reusesAuthenticationAndCopiesCachedValues() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        UserData first = user();
        cache.authenticate(first, "client", this::authenticate);
        first.setId("changed");
        first.setAccessToken("changed");

        UserData second = user();
        cache.authenticate(second, "client", this::authenticate);

        assertEquals(1, authentications);
        assertEquals("user-id", second.getId());
        assertEquals(token(NOW.plusSeconds(120)), second.getAccessToken());
    }

    @Test
    void refreshesBeforeExpiryEvenWhenFrequentlyAccessed() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        cache.authenticate(user(), "client", this::authenticate);
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        cache.authenticate(user(), "client", this::authenticate);
        assertEquals(1, authentications);

        when(clock.instant()).thenReturn(NOW.plusSeconds(90));
        cache.authenticate(user(), "client", this::authenticate);
        assertEquals(2, authentications);
    }

    @Test
    void refreshesAfterFiveMinutesWithoutExtendingLifetimeOnAccess() throws Exception {
        Ticker ticker = mock(Ticker.class);
        FunctionalTestUserTokenCache boundedCache = new FunctionalTestUserTokenCache(
            Clock.fixed(NOW, ZoneOffset.UTC), ticker);
        FunctionalTestUserTokenCache.Authenticator authenticator = (user, client) -> {
            authentications++;
            user.setAccessToken(token(NOW.plusSeconds(3600)));
        };
        when(ticker.read()).thenReturn(0L);
        boundedCache.authenticate(user(), "client", authenticator);
        when(ticker.read()).thenReturn(TimeUnit.MINUTES.toNanos(4));
        boundedCache.authenticate(user(), "client", authenticator);
        assertEquals(1, authentications);
        when(ticker.read()).thenReturn(TimeUnit.MINUTES.toNanos(5));
        boundedCache.authenticate(user(), "client", authenticator);
        assertEquals(2, authentications);
    }

    @Test
    void separatesClientsAndCredentials() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        cache.authenticate(user(), "first-client", this::authenticate);
        cache.authenticate(user(), "second-client", this::authenticate);
        UserData changedPassword = user();
        changedPassword.setPassword("other-password");
        cache.authenticate(changedPassword, "first-client", this::authenticate);
        assertEquals(3, authentications);
    }

    @Test
    void doesNotCacheMissingMalformedOrExpiredTokens() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        String[] tokens = {null, "malformed", JWT.create().sign(Algorithm.HMAC256("test")), token(NOW)};
        for (String token : tokens) {
            FunctionalTestUserTokenCache isolated = new FunctionalTestUserTokenCache(Clock.fixed(NOW, ZoneOffset.UTC));
            FunctionalTestUserTokenCache.Authenticator authenticator = (user, client) -> {
                authentications++;
                user.setAccessToken(token);
            };
            isolated.authenticate(user(), "client", authenticator);
            isolated.authenticate(user(), "client", authenticator);
        }
        assertEquals(8, authentications);
    }

    @Test
    void retriesAuthenticationAfterFailure() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        assertThrows(ExecutionException.class, () -> cache.authenticate(user(), "client", (user, client) -> {
            throw new ExecutionException(new IllegalStateException("IDAM unavailable"));
        }));
        cache.authenticate(user(), "client", this::authenticate);
        assertEquals(1, authentications);
    }

    private void authenticate(UserData user, String client) {
        authentications++;
        user.setId("user-id");
        user.setAccessToken(token(clock.instant().plusSeconds(120)));
    }

    private UserData user() {
        return new UserData("user", "password");
    }

    private String token(Instant expiry) {
        return JWT.create().withExpiresAt(Date.from(expiry)).sign(Algorithm.HMAC256("test"));
    }
}
