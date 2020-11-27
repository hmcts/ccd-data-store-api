package uk.gov.hmcts.ccd.config;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.RequestParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint;
import uk.gov.hmcts.ccd.endpoint.ui.QueryEndpoint;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public Docket apiV1External() {
        return getNewDocketForPackageOf(CaseDetailsEndpoint.class, "v1_external", apiV1Info());
    }

    @Bean
    public Docket apiV2External() {

        return getNewDocketForPackageOf(CaseController.class, "v2_external", apiV2Info());
    }

    @Bean
    public Docket apiV1Internal() {

        return getNewDocketForPackageOf(QueryEndpoint.class, "v1_internal", apiV1Info());
    }

    @Bean
    public Docket apiV2Internal() {
        return getNewDocketForPackageOf(UICaseController.class, "v2_internal", apiV2Info());
    }

    private Docket getNewDocketForPackageOf(Class<?> klazz, String groupName, ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .select()
                .apis(RequestHandlerSelectors.basePackage(klazz.getPackage().getName()))
                .paths(PathSelectors.any())
                .build().useDefaultResponseMessages(false)
                .apiInfo(apiInfo)
                .globalRequestParameters(Arrays.asList(headerAuthorization(), headerServiceAuthorization()));
    }

    private ApiInfo apiV1Info() {
        return new ApiInfoBuilder()
            .title("Core Case Data - Data store API")
            .description("Create, modify, retrieve and search cases")
            .license("")
            .licenseUrl("")
            .version("1.0.1")
            .contact(new Contact("CCD",
                                 "https://tools.hmcts.net/confluence/display/RCCD/Reform%3A+Core+Case+Data+Home",
                                 "corecasedatateam@hmcts.net"))
            .termsOfServiceUrl("")
            .build();
    }

    private ApiInfo apiV2Info() {
        return new ApiInfoBuilder()
            .title("CCD Data Store API")
            .description("Create, modify, retrieve and search cases")
            .license("MIT")
            .licenseUrl("https://opensource.org/licenses/MIT")
            .version("2-beta")
            .contact(new Contact("CCD",
                                 "https://tools.hmcts.net/confluence/display/RCCD/Reform%3A+Core+Case+Data+Home",
                                 "corecasedatateam@hmcts.net"))
            .build();
    }

    private RequestParameter headerAuthorization() {
        return new RequestParameterBuilder()
            .name("Authorization")
            .description("Keyword `Bearer` followed by a valid IDAM user token")
            .in("header")
            .accepts(Collections.singleton(MediaType.APPLICATION_JSON))
            .required(true)
            .build();
    }

    private RequestParameter headerServiceAuthorization() {
        return new RequestParameterBuilder()
            .name("ServiceAuthorization")
            .description("Valid Service-to-Service JWT token for a whitelisted micro-service")
            .in("header")
            .accepts(Collections.singleton(MediaType.APPLICATION_JSON))
            .required(true)
            .build();
    }
}
