package uk.gov.hmcts.ccd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("ccd.messaging")
@Data
public class MessagingProperties {

    private Map<String, String> typeMappings;
}
