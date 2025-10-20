package vn.edu.iuh.fit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Thymeleaf configuration for email templates
 */
@Configuration
public class ThymeleafConfig {

  @Bean
  @Primary
  public SpringResourceTemplateResolver emailTemplateResolver() {
    SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setPrefix("classpath:/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setCacheable(false); // Disable cache for development
    templateResolver.setOrder(1);
    templateResolver.setCheckExistence(true); // Verify template exists
    return templateResolver;
  }

  @Bean
  @Primary
  public SpringTemplateEngine templateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(emailTemplateResolver());
    templateEngine.setEnableSpringELCompiler(true);
    return templateEngine;
  }
}
