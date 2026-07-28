/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
public class CustomOauth2AuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final static String processUrl = "/login/oauth2/code/{" + AbstractCustomOauth2Client.REGISTRATION_ID + "}";

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final CustomOAuth2ClientFactory clientFactory;

    private final AntPathRequestMatcher authorizationRequestMatcher = new AntPathRequestMatcher(processUrl);

    public CustomOauth2AuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository, AuthenticationSuccessHandler authenticationSuccessHandler) {
        this(clientRegistrationRepository, authenticationSuccessHandler, CustomOAuth2ClientFactory.getInstance());
    }

    CustomOauth2AuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository,
                                     AuthenticationSuccessHandler authenticationSuccessHandler,
                                     CustomOAuth2ClientFactory clientFactory) {
        super(processUrl);
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.clientFactory = clientFactory;
        this.setAuthenticationSuccessHandler(authenticationSuccessHandler);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String registrationId = resolveRegistrationId(request);
        Optional<AbstractCustomOauth2Client> client = clientFactory.findConfigured(registrationId);
        if (!client.isPresent() || clientRegistrationRepository.findByRegistrationId(registrationId) == null) {
            throw new AuthenticationServiceException("Custom OAuth2 client is not configured: " + registrationId);
        }
        Authentication authentication = client.get().getUserInfo(request, response);
        if (authentication == null) {
            throw new AuthenticationServiceException("Custom OAuth2 provider returned no authentication: " + registrationId);
        }
        return authentication;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String registrationId = resolveRegistrationId(request);
        return StringUtils.isNotBlank(registrationId)
                && clientFactory.findConfigured(registrationId).isPresent()
                && clientRegistrationRepository.findByRegistrationId(registrationId) != null;
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        if (this.authorizationRequestMatcher.matches(request)) {
            return this.authorizationRequestMatcher.matcher(request).getVariables()
                    .get(AbstractCustomOauth2Client.REGISTRATION_ID);
        }
        return null;
    }
}
