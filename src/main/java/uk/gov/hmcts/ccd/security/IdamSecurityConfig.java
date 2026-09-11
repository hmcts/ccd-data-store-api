package uk.gov.hmcts.ccd.security;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Getter
@Setter
public class IdamSecurityConfig {

    @Value("#{'${idam.security.allowed-issuers}'.split(',')}")
    private List<String> allowedIssuers;
}