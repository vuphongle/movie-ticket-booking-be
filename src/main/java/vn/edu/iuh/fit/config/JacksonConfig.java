package vn.edu.iuh.fit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Hibernate5Module to handle proxy objects
        Hibernate5Module hibernate5Module = new Hibernate5Module();
        // Configure to serialize lazy-loaded properties as null instead of throwing error
        hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate5Module.configure(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION, false);
        hibernate5Module.configure(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        
        mapper.registerModule(hibernate5Module);
        
        return mapper;
    }
}