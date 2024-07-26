package awt.dms.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;

@TestConfiguration(proxyBeanMethods = false)
public class KeycloakContainerConfig {

    @Bean
    KeycloakContainer keycloakContainer(final DynamicPropertyRegistry registry) {
        final KeycloakContainer keycloakContainer = new KeycloakContainer();
        keycloakContainer.start();

        registry.add("keycloak.server.external-url", keycloakContainer::getAuthServerUrl);
        registry.add("keycloak.server.internal-url", keycloakContainer::getAuthServerUrl);
        registry.add("keycloak.dev-portal-realm", () -> "master");

        return keycloakContainer;
    }
}
