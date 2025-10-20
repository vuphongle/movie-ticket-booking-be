package vn.edu.iuh.fit.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Getter
@Configuration
public class SendPulseConfig {

  @Value("${sendpulse.api.url}")
  private String apiUrl;

  @Value("${sendpulse.api.client-id}")
  private String clientId;

  @Value("${sendpulse.api.client-secret}")
  private String clientSecret;

  @Value("${sendpulse.mail.from.email}")
  private String fromEmail;

  @Value("${sendpulse.mail.from.name}")
  private String fromName;

  @Bean
  public WebClient sendPulseWebClient() {
    return WebClient.builder().baseUrl(apiUrl).build();
  }
}
