package datart.security.oauth2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import datart.core.common.Application;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WeChatOauth2Client extends AbstractCustomOauth2Client {

    public static final String REGISTRATION_ID = "wechat";
    private static final String AUTHORIZATION_URI = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String TOKEN_URI = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USER_INFO_URI = "https://api.weixin.qq.com/sns/userinfo";

    @Override
    public String getRegistrationId() {
        return REGISTRATION_ID;
    }

    @Override
    public void authorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            URIBuilder uri = new URIBuilder(AUTHORIZATION_URI)
                    .addParameter("scope", "snsapi_login")
                    .addParameter("response_type", "code")
                    .addParameter("lang", "cn")
                    .addParameter("appid", getClientRegistration().getClientId())
                    .addParameter("state", createState(request))
                    .addParameter("redirect_uri", getRedirectUrl());
            response.sendRedirect(uri.build().toString());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create WeChat authorization request", e);
        }
    }

    @Override
    public Authentication getUserInfo(HttpServletRequest request, HttpServletResponse response) {
        try {
            verifyAndConsumeState(request);
            JSONObject token = getJson(TOKEN_URI, new URIBuilder(TOKEN_URI)
                    .addParameter("grant_type", "authorization_code")
                    .addParameter("appid", getClientRegistration().getClientId())
                    .addParameter("secret", getClientRegistration().getClientSecret())
                    .addParameter("code", request.getParameter("code")));
            String accessToken = required(token, "access_token");
            String openId = required(token, "openid");
            JSONObject user = getJson(USER_INFO_URI, new URIBuilder(USER_INFO_URI)
                    .addParameter("access_token", accessToken)
                    .addParameter("openid", openId)
                    .addParameter("lang", "zh_CN"));
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(NAME, required(user, "openid"));
            attributes.put(EMAIL, user.getString("unionid"));
            attributes.put(AVATAR, user.getString("headimgurl"));
            DefaultOAuth2User principal = new DefaultOAuth2User(Collections.emptyList(), attributes, NAME);
            return new OAuth2AuthenticationToken(principal, Collections.emptyList(), getRegistrationId());
        } catch (Exception e) {
            throw new AuthenticationServiceException("WeChat authentication failed", e);
        }
    }

    @Override
    public void addClientRegistration(OAuth2ClientProperties properties) {
        String registrationId = getRegistrationId();
        if (properties == null || !properties.getRegistration().containsKey(registrationId)) {
            return;
        }
        OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
        provider.setAuthorizationUri(AUTHORIZATION_URI);
        provider.setTokenUri(TOKEN_URI);
        provider.setUserInfoUri(USER_INFO_URI);
        properties.getProvider().put(registrationId, provider);
        OAuth2ClientProperties.Registration registration = properties.getRegistration().get(registrationId);
        registration.setAuthorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        registration.setRedirectUri(redirectUri());
    }

    private String getRedirectUrl() {
        String registrationId = getRegistrationId();
        String url = Application.getProperty("spring.security.oauth2.client.registration."
                + registrationId + ".call-back-url");
        if (StringUtils.isBlank(url)) {
            url = Application.getServerPrefix();
        }
        return StringUtils.removeEnd(url, "/") + redirectUri();
    }

    private String redirectUri() {
        return "/login/oauth2/code/" + getRegistrationId();
    }

    private JSONObject getJson(String endpoint, URIBuilder uri) throws Exception {
        HttpGet request = new HttpGet(uri.build());
        HttpResponse response = getHttpClient().execute(request);
        String entity = EntityUtils.toString(response.getEntity());
        JSONObject json = JSON.parseObject(entity);
        Integer errorCode = json.getInteger("errcode");
        if (response.getStatusLine().getStatusCode() >= 400 || (errorCode != null && errorCode != 0)) {
            throw new IllegalStateException(endpoint + " returned an OAuth2 error");
        }
        return json;
    }

    private String required(JSONObject json, String property) {
        String value = json.getString(property);
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException("WeChat response is missing " + property);
        }
        return value;
    }
}
