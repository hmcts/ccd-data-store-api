package uk.gov.hmcts.ccd.config;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint;
import uk.gov.hmcts.ccd.endpoint.ui.QueryEndpoint;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi apiV1External() {
        return getNewGroupedOpenApiForPackageOf(CaseDetailsEndpoint.class, "v1_external");
    }

    @Bean
    public GroupedOpenApi apiV2External() {

        return getNewGroupedOpenApiForPackageOf(CaseController.class, "v2_external");
    }

    @Bean
    public GroupedOpenApi apiV1Internal() {

        return getNewGroupedOpenApiForPackageOf(QueryEndpoint.class, "v1_internal");
    }

    @Bean
    public GroupedOpenApi apiV2Internal() {
        return getNewGroupedOpenApiForPackageOf(UICaseController.class, "v2_internal");
    }

    private GroupedOpenApi getNewGroupedOpenApiForPackageOf(Class<?> klazz, String groupName) {
        return GroupedOpenApi.builder()
                .group(groupName)
                .packagesToScan(klazz.getPackage().getName())
                .build();
    }

    //CCD-3509 CVE-2021-22044 required to fix null pointers in integration tests,
    //conflict in Springfox after Springboot 2.6.10
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
        ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
        EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
        WebEndpointProperties webEndpointProperties, Environment environment) {

        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping = this.shouldRegisterLinksMapping(webEndpointProperties, environment,
            basePath);
        return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
            corsProperties.toCorsConfiguration(),
            new EndpointLinksResolver(allEndpoints, basePath),
            shouldRegisterLinksMapping);
    }

    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties, Environment environment,
                                               String basePath) {
        return webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.isNotBlank(basePath)
            || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}
