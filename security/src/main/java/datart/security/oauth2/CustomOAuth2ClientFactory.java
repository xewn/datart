package datart.security.oauth2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

public final class CustomOAuth2ClientFactory {

    private static class Holder {
        private static final CustomOAuth2ClientFactory INSTANCE = load();
    }

    private final Map<String, AbstractCustomOauth2Client> clients;

    CustomOAuth2ClientFactory(Iterable<AbstractCustomOauth2Client> discovered) {
        Map<String, AbstractCustomOauth2Client> loaded = new TreeMap<>();
        for (AbstractCustomOauth2Client client : discovered) {
            if (client == null || StringUtils.isBlank(client.getRegistrationId())) {
                throw new IllegalStateException("Custom OAuth2 registration ID cannot be blank");
            }
            String registrationId = client.getRegistrationId();
            if (loaded.putIfAbsent(registrationId, client) != null) {
                throw new IllegalStateException("Duplicate custom OAuth2 registration ID: " + registrationId);
            }
        }
        clients = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
    }

    public static CustomOAuth2ClientFactory load() {
        return new CustomOAuth2ClientFactory(ServiceLoader.load(AbstractCustomOauth2Client.class));
    }

    public static CustomOAuth2ClientFactory getInstance() {
        return Holder.INSTANCE;
    }

    public Set<String> registrationIds() {
        return clients.keySet();
    }

    public Optional<AbstractCustomOauth2Client> find(String registrationId) {
        return Optional.ofNullable(clients.get(registrationId));
    }

    public Optional<AbstractCustomOauth2Client> findConfigured(String registrationId) {
        AbstractCustomOauth2Client client = clients.get(registrationId);
        if (client == null) {
            return Optional.empty();
        }
        try {
            client.getClientRegistration();
            return Optional.of(client);
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    public void bind(Map<String, ClientRegistration> registrations) {
        Map<String, ClientRegistration> safeRegistrations = registrations == null
                ? Collections.emptyMap()
                : registrations;
        clients.forEach((id, client) -> client.bind(safeRegistrations.get(id)));
    }
}
