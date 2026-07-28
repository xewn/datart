package datart.security.oauth2;

/**
 * Compatibility alias for deployments configured with the historical typo.
 */
@Deprecated
public class WeChartOauth2Client extends WeChatOauth2Client {

    public static final String REGISTRATION_ID = "wechart";

    @Override
    public String getRegistrationId() {
        return REGISTRATION_ID;
    }
}
