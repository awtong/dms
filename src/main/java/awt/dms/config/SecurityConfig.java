package awt.dms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class SecurityConfig {

  @Bean
  WebSecurityCustomizer ignoreResources() {
    return webSecurity -> webSecurity.ignoring().anyRequest();
  }
}
