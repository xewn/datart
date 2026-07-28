package datart.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CustomOAuth2FiltersTest {

    @Test
    void authorizationFilterDelegatesOnlyConfiguredCustomClient() throws Exception {
        CustomOAuth2ClientFactoryTest.StubClient client = new CustomOAuth2ClientFactoryTest.StubClient("custom");
        CustomOAuth2ClientFactory factory = new CustomOAuth2ClientFactory(Collections.singletonList(client));
        ClientRegistration registration = CustomOAuth2ClientFactoryTest.registration("custom");
        factory.bind(Collections.singletonMap("custom", registration));
        ClientRegistrationRepository repository = id -> "custom".equals(id) ? registration : null;
        CustomOAuth2AuthorizationRequestRedirectFilter filter =
                new CustomOAuth2AuthorizationRequestRedirectFilter(repository, factory);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("/oauth2/authorization/custom"), response, chain);
        assertEquals(1, client.authorizationRequests);
        verifyNoInteractions(chain);

        MockHttpServletRequest unknown = request("/oauth2/authorization/unknown");
        filter.doFilter(unknown, response, chain);
        verify(chain).doFilter(unknown, response);
    }

    @Test
    void authenticationFilterMatchesAndAuthenticatesOnlyConfiguredClient() throws Exception {
        CustomOAuth2ClientFactoryTest.StubClient client = new CustomOAuth2ClientFactoryTest.StubClient("custom");
        CustomOAuth2ClientFactory factory = new CustomOAuth2ClientFactory(Collections.singletonList(client));
        ClientRegistration registration = CustomOAuth2ClientFactoryTest.registration("custom");
        factory.bind(Collections.singletonMap("custom", registration));
        ClientRegistrationRepository repository = id -> "custom".equals(id) ? registration : null;
        ExposedAuthenticationFilter filter = new ExposedAuthenticationFilter(repository,
                mock(AuthenticationSuccessHandler.class), factory);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(filter.matches(request("/login/oauth2/code/unknown"), response));
        MockHttpServletRequest callback = request("/login/oauth2/code/custom");
        assertTrue(filter.matches(callback, response));
        Authentication authentication = filter.attemptAuthentication(callback, response);
        assertNotNull(authentication);
        assertEquals("custom", ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken)
                authentication).getAuthorizedClientRegistrationId());
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setServletPath(uri);
        return request;
    }

    private static class ExposedAuthenticationFilter extends CustomOauth2AuthenticationFilter {
        ExposedAuthenticationFilter(ClientRegistrationRepository repository,
                                    AuthenticationSuccessHandler successHandler,
                                    CustomOAuth2ClientFactory factory) {
            super(repository, successHandler, factory);
        }

        boolean matches(MockHttpServletRequest request, MockHttpServletResponse response) {
            return super.requiresAuthentication(request, response);
        }
    }
}
