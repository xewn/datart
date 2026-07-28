package datart.security.oauth2;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractCustomOauth2Client {

    public static final String NAME = "username";
    public static final String EMAIL = "email";
    public static final String AVATAR = "avatar";
    public static final String REGISTRATION_ID = "registrationId";

    private volatile HttpClient httpClient;
    private volatile ClientRegistration clientRegistration;

    public abstract String getRegistrationId();

    public void addClientRegistration(OAuth2ClientProperties properties) {
    }

    public abstract void authorizationRequest(HttpServletRequest request, HttpServletResponse response);

    public abstract Authentication getUserInfo(HttpServletRequest request, HttpServletResponse response);

    public final ClientRegistration getClientRegistration() {
        ClientRegistration registration = clientRegistration;
        if (registration == null) {
            throw new IllegalStateException("OAuth2 client is not configured: " + getRegistrationId());
        }
        return registration;
    }

    final void bind(ClientRegistration registration) {
        if (registration != null && !getRegistrationId().equals(registration.getRegistrationId())) {
            throw new IllegalArgumentException("OAuth2 registration ID does not match provider ID");
        }
        clientRegistration = registration;
    }

    protected final HttpClient getHttpClient() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = HttpClients.createSystem();
                    httpClient = client;
                }
            }
        }
        return client;
    }
}
