package uk.gov.hmcts.ccd;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage(uk.gov.hmcts.ccd.CoreCaseDataApplication.class.getPackage().getName()))
            .build()
            .useDefaultResponseMessages(false)
            .apiInfo(apiInfo())
            .host("case-data-app.dev.ccd.reform.hmcts.net:4451");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Core Case Data - Data store API")
            .description("Create, modify, retrieve and search cases")
            .license("")
            .licenseUrl("")
            .version("1.0.1")
            .contact(new Contact("CCD", "https://tools.hmcts.net/confluence/display/RCCD/Reform%3A+Core+Case+Data+Home", "corecasedatateam@hmcts.net"))
            .termsOfServiceUrl("")
            .build();
    }
}
