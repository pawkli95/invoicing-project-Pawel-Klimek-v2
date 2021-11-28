package pl.futurecollars.invoicing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("pl.futurecollars.invoicing"))
                .paths(PathSelectors.any())
                .build()
                .tags(
                        new Tag("invoice-controller", "Controller used to manage invoices"),
                        new Tag("tax-calculator-controller", "Controller used to calculate taxes"),
                        new Tag("company-controller", "Controller used to manage companies"),
                        new Tag("auth-controller", "Controller used to authenticate users"),
                        new Tag("user-controller", "Controller used to manage users")
                )
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .description("Application to manage invoices")
                .title("Invoicing System by Paweł Klimek")
                .license("No license")
                .contact(new Contact(
                        "Paweł Klimek",
                        "https://github.com/pawkli95",
                        "pawkli95@gmail.com"))
                .build();

    }
}
