package datart.security.oauth2;

import org.springframework.security.authentication.AuthenticationServiceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

final class OAuth2StateRepository {

    private static final String SESSION_PREFIX = OAuth2StateRepository.class.getName() + ".";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration ttl;

    OAuth2StateRepository() {
        this(Clock.systemUTC(), DEFAULT_TTL);
    }

    OAuth2StateRepository(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    String create(HttpServletRequest request, String registrationId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        HttpSession session = request.getSession(true);
        synchronized (session) {
            session.setAttribute(key(registrationId), new StoredState(state, clock.millis()));
        }
        return state;
    }

    void consume(HttpServletRequest request, String registrationId) {
        HttpSession session = request.getSession(false);
        Object stored = null;
        if (session != null) {
            synchronized (session) {
                stored = session.getAttribute(key(registrationId));
                session.removeAttribute(key(registrationId));
            }
        }
        String actual = request.getParameter("state");
        if (!(stored instanceof StoredState)
                || actual == null
                || clock.millis() - ((StoredState) stored).createdAt > ttl.toMillis()
                || !MessageDigest.isEqual(((StoredState) stored).value.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthenticationServiceException("Failed to verify the state parameter");
        }
    }

    private String key(String registrationId) {
        return SESSION_PREFIX + registrationId;
    }

    private static class StoredState implements java.io.Serializable {
        private final String value;
        private final long createdAt;

        private StoredState(String value, long createdAt) {
            this.value = value;
            this.createdAt = createdAt;
        }
    }
}
