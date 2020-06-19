package uk.gov.hmcts.ccd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;
import uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfiguration {

    @Bean
    public Docket apiV1() {
        return getNewDocketForPackageOf(CaseDetailsEndpoint.class, "v1", apiV1Info());
    }

    @Bean
    public Docket apiV2() {
        return getNewDocketForPackageOf(CaseController.class, "v2", apiV2Info());
    }

    private Docket getNewDocketForPackageOf(Class<?> klazz, String groupName, ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .select()
                .apis(RequestHandlerSelectors.basePackage(klazz.getPackage().getName()))
                .paths(PathSelectors.any())
                .build().useDefaultResponseMessages(false)
                .apiInfo(apiInfo)
                .globalOperationParameters(Arrays.asList(headerAuthorization(), headerServiceAuthorization()));
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

    private Parameter headerAuthorization() {
        return new ParameterBuilder()
            .name("Authorization")
            .description("Keyword `Bearer` followed by a valid IDAM user token")
            .modelRef(new ModelRef("string"))
            .parameterType("header")
            .required(true)
            .build();
    }

    private Parameter headerServiceAuthorization() {
        return new ParameterBuilder()
            .name("ServiceAuthorization")
            .description("Valid Service-to-Service JWT token for a whitelisted micro-service")
            .modelRef(new ModelRef("string"))
            .parameterType("header")
            .required(true)
            .build();
    }
}
