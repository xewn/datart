package datart.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuth2StateRepositoryTest {

    @Test
    void bindsStateToSessionAndConsumesItOnce() {
        OAuth2StateRepository repository = new OAuth2StateRepository();
        MockHttpServletRequest authorization = new MockHttpServletRequest();
        String state = repository.create(authorization, "custom");
        MockHttpServletRequest callback = callback(authorization, state);

        repository.consume(callback, "custom");

        assertThrows(AuthenticationServiceException.class,
                () -> repository.consume(callback, "custom"));
        assertThrows(AuthenticationServiceException.class,
                () -> repository.consume(new MockHttpServletRequest(), "custom"));
    }

    @Test
    void rejectsMismatchedAndExpiredState() {
        Clock issuedAt = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        OAuth2StateRepository issuer = new OAuth2StateRepository(issuedAt, Duration.ofMinutes(10));
        MockHttpServletRequest authorization = new MockHttpServletRequest();
        String mismatchedState = issuer.create(authorization, "custom");

        assertThrows(AuthenticationServiceException.class,
                () -> issuer.consume(callback(authorization, mismatchedState + "x"), "custom"));

        MockHttpServletRequest expiredAuthorization = new MockHttpServletRequest();
        String expiredState = issuer.create(expiredAuthorization, "custom");
        OAuth2StateRepository expired = new OAuth2StateRepository(
                Clock.fixed(Instant.parse("2024-01-01T00:11:00Z"), ZoneOffset.UTC), Duration.ofMinutes(10));
        MockHttpServletRequest expiredCallback = callback(expiredAuthorization, expiredState);
        assertThrows(AuthenticationServiceException.class,
                () -> expired.consume(expiredCallback, "custom"));
    }

    private MockHttpServletRequest callback(MockHttpServletRequest authorization, String state) {
        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setSession(authorization.getSession());
        callback.setParameter("state", state);
        return callback;
    }
}
