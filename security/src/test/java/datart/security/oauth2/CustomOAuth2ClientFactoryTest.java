package datart.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomOAuth2ClientFactoryTest {

    @Test
    void rejectsBlankAndDuplicateRegistrationIds() {
        assertThrows(IllegalStateException.class,
                () -> new CustomOAuth2ClientFactory(Collections.singletonList(new StubClient(" "))));
        assertThrows(IllegalStateException.class,
                () -> new CustomOAuth2ClientFactory(Arrays.asList(new StubClient("same"), new StubClient("same"))));
    }

    @Test
    void exposesOnlyConfiguredClients() {
        StubClient client = new StubClient("custom");
        CustomOAuth2ClientFactory factory = new CustomOAuth2ClientFactory(Collections.singletonList(client));

        assertTrue(factory.find("custom").isPresent());
        assertFalse(factory.findConfigured("custom").isPresent());
        factory.bind(Collections.singletonMap("custom", registration("custom")));
        assertTrue(factory.findConfigured("custom").isPresent());
        assertFalse(factory.findConfigured("unknown").isPresent());
        assertThrows(UnsupportedOperationException.class,
                () -> factory.registrationIds().add("mutated"));
    }

    @Test
    void discoversBuiltInClientsThroughSpi() {
        CustomOAuth2ClientFactory factory = CustomOAuth2ClientFactory.load();

        assertTrue(factory.registrationIds().contains("dingtalk"));
        assertTrue(factory.registrationIds().contains("wechat"));
        assertTrue(factory.registrationIds().contains("wechart"));
        assertEquals(3, factory.registrationIds().size());
    }

    @Test
    void repositoryBindsOnlyConfiguredSpiClients() {
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
        registration.setClientId("client");
        registration.setClientSecret("secret");
        properties.getRegistration().put("dingtalk", registration);
        ClientRegistrationRepositoryImpl repository = new ClientRegistrationRepositoryImpl();

        repository.setOAuth2ClientProperties(properties);

        assertTrue(CustomOAuth2ClientFactory.getInstance().findConfigured("dingtalk").isPresent());
        assertFalse(CustomOAuth2ClientFactory.getInstance().findConfigured("wechat").isPresent());
        assertEquals("client", repository.findByRegistrationId("dingtalk").getClientId());
    }

    @Test
    void repositorySupportsLegacyWechartRegistration() {
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
        registration.setClientId("legacy-client");
        registration.setClientSecret("secret");
        properties.getRegistration().put("wechart", registration);
        ClientRegistrationRepositoryImpl repository = new ClientRegistrationRepositoryImpl();

        repository.setOAuth2ClientProperties(properties);

        assertTrue(CustomOAuth2ClientFactory.getInstance().findConfigured("wechart").isPresent());
        assertEquals("legacy-client", repository.findByRegistrationId("wechart").getClientId());
    }

    static ClientRegistration registration(String id) {
        return ClientRegistration.withRegistrationId(id)
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/authorize")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/user")
                .userNameAttributeName("username")
                .clientName(id)
                .build();
    }

    static class StubClient extends AbstractCustomOauth2Client {
        private final String registrationId;
        int authorizationRequests;

        StubClient(String registrationId) {
            this.registrationId = registrationId;
        }

        @Override
        public String getRegistrationId() {
            return registrationId;
        }

        @Override
        public void addClientRegistration(OAuth2ClientProperties properties) {
        }

        @Override
        public void authorizationRequest(HttpServletRequest request, HttpServletResponse response) {
            authorizationRequests++;
        }

        @Override
        public Authentication getUserInfo(HttpServletRequest request, HttpServletResponse response) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(NAME, "user");
            DefaultOAuth2User user = new DefaultOAuth2User(Collections.emptyList(), attributes, NAME);
            return new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                    user, Collections.emptyList(), registrationId);
        }
    }
}
