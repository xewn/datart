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

import com.aliyun.dingtalkcontact_1_0.models.GetUserHeaders;
import com.aliyun.dingtalkcontact_1_0.models.GetUserResponseBody;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import datart.core.common.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;

@Slf4j
public class DingTalkOauth2Client extends AbstractCustomOauth2Client {

    public static final String REGISTRATION_ID = "dingtalk";

    private static final String authorizationUri = "https://login.dingtalk.com/oauth2/auth";

    private static final String tokenUri = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";

    private static final String userInfoUri = "https://api.dingtalk.com/v1.0/contact/users/me";

    private static final String redirectUri = "/login/oauth2/code/" + REGISTRATION_ID;

    @Override
    public void authorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            URIBuilder uriBuilder = new URIBuilder(authorizationUri);
            uriBuilder.addParameter("prompt", "consent");
            uriBuilder.addParameter("scope", "openid");
            uriBuilder.addParameter("response_type", "code");
            uriBuilder.addParameter("client_id", getClientRegistration().getClientId());
            uriBuilder.addParameter("state", createState(request));
            uriBuilder.addParameter("redirect_uri", getRedirectUrl());
            response.sendRedirect(uriBuilder.build().toString());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create DingTalk authorization request", e);
        }
    }

    private String getRedirectUrl() {
        String url = Application.getProperty("spring.security.oauth2.client.registration.dingtalk.call-back-url");
        if (StringUtils.isBlank(url)) {
            url = Application.getServerPrefix();
        }
        url = StringUtils.removeEnd(url, "/");
        url = url + redirectUri;
        return url;
    }

    @Override
    public String getRegistrationId() {
        return REGISTRATION_ID;
    }

    @Override
    public OAuth2AuthenticationToken getUserInfo(HttpServletRequest request, HttpServletResponse response) {
        try {
            String authCode = request.getParameter("authCode");
            verifyAndConsumeState(request);
            String accessToken = getAccessToken(authCode);
            return getUserinfo(accessToken);
        } catch (Exception e) {
            throw new AuthenticationServiceException("DingTalk authentication failed", e);
        }
    }

    @Override
    public void addClientRegistration(OAuth2ClientProperties properties) {
        if (properties == null) {
            return;
        }
        if (properties.getRegistration().containsKey(REGISTRATION_ID)) {
            properties.getProvider()
                    .put(REGISTRATION_ID, creatProvider());
            OAuth2ClientProperties.Registration registration = properties.getRegistration().get(REGISTRATION_ID);
            registration.setAuthorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
            registration.setRedirectUri(redirectUri);
        }
    }


    private static OAuth2ClientProperties.Provider creatProvider() {
        OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
        provider.setTokenUri(tokenUri);
        provider.setUserInfoUri(userInfoUri);
        provider.setAuthorizationUri(authorizationUri);
        return provider;
    }

    private com.aliyun.dingtalkoauth2_1_0.Client authClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkoauth2_1_0.Client(config);
    }

    private String getAccessToken(String authCode) throws Exception {
        ClientRegistration clientRegistration = getClientRegistration();
        com.aliyun.dingtalkoauth2_1_0.Client client = authClient();
        GetUserTokenRequest getUserTokenRequest = new GetUserTokenRequest()
                .setClientId(clientRegistration.getClientId())
                .setClientSecret(clientRegistration.getClientSecret())
                .setCode(authCode)
                .setGrantType("authorization_code");
        GetUserTokenResponse getUserTokenResponse = client.getUserToken(getUserTokenRequest);
        //获取用户个人token
        return getUserTokenResponse.getBody().getAccessToken();
    }

    private com.aliyun.dingtalkcontact_1_0.Client contactClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkcontact_1_0.Client(config);
    }

    private OAuth2AuthenticationToken getUserinfo(String accessToken) throws Exception {
        com.aliyun.dingtalkcontact_1_0.Client client = contactClient();
        GetUserHeaders getUserHeaders = new GetUserHeaders();
        getUserHeaders.xAcsDingtalkAccessToken = accessToken;
        GetUserResponseBody userResponseBody = client.getUserWithOptions("me", getUserHeaders, new RuntimeOptions()).getBody();
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(NAME, userResponseBody.getNick());
        attributes.put(EMAIL, userResponseBody.getEmail());
        attributes.put(AVATAR, userResponseBody.getAvatarUrl());
        DefaultOAuth2User auth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, NAME);
        return new OAuth2AuthenticationToken(auth2User, Collections.emptyList(), REGISTRATION_ID);
    }


}
