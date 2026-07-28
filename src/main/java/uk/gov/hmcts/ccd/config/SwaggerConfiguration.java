package uk.gov.hmcts.ccd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OpenApiHateoasLinksCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint;
import uk.gov.hmcts.ccd.endpoint.ui.QueryEndpoint;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SwaggerConfiguration {

    private static final Set<String> NULLABLE_LINK_PROPERTIES = Set.of(
        "deprecation", "hreflang", "name", "profile", "title", "type"
    );

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

    @Bean(name = "linksSchemaCustomizer")
    public GlobalOpenApiCustomizer linksSchemaCustomizer(SpringDocConfigProperties springDocConfigProperties) {
        OpenApiHateoasLinksCustomizer hateoasCustomizer =
            new OpenApiHateoasLinksCustomizer(springDocConfigProperties);
        return openApi -> {
            hateoasCustomizer.customise(openApi);
            preserveLinkPropertyNullability(openApi);
        };
    }

    private void preserveLinkPropertyNullability(OpenAPI openApi) {
        Schema<?> linkSchema = openApi.getComponents().getSchemas().get("Link");
        if (linkSchema == null || linkSchema.getProperties() == null) {
            return;
        }

        NULLABLE_LINK_PROPERTIES.forEach(propertyName -> {
            Schema<?> propertySchema = linkSchema.getProperties().get(propertyName);
            if (propertySchema != null) {
                propertySchema.setType(null);
                propertySchema.setTypes(new LinkedHashSet<>(List.of("string", "null")));
            }
        });
    }

    //CCD-3509 CVE-2021-22044 required to fix null pointers in integration tests,
    //conflict in Springfox after Springboot 2.6.10
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
        EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
        WebEndpointProperties webEndpointProperties, Environment environment) {

        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
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
