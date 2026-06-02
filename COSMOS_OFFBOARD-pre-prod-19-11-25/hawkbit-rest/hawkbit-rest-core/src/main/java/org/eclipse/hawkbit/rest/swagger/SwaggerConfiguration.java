package org.eclipse.hawkbit.rest.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.eclipse.hawkbit.rest.RequestInterceptor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Configuration class for Swagger 3 (OpenAPI) documentation.
 * <p>
 * Defines OpenAPI groups, customizers, and security schemes for
 * device, management, and system APIs. Also configures Swagger UI redirects.
 */
@OpenAPIDefinition
@Configuration
public class SwaggerConfiguration {

    /**
     * Defines the OpenAPI group for device-related endpoints.
     *
     * @param securityOpenApiCustomizer customizer for security schemes
     * @param deviceCustomizer          customizer for device API documentation
     * @return GroupedOpenApi for device endpoints
     */
    @Bean
    public GroupedOpenApi deviceApi(
            @Qualifier("securityOpenApiCustomizer") OpenApiCustomizer securityOpenApiCustomizer,
            @Qualifier("deviceCustomizer") OpenApiCustomizer deviceCustomizer) {
        return buildGroupedOpenApi("device",
                new String[]{"/device/**", "/api/v1/controllers/**"},
                new String[]{},
                deviceCustomizer, securityOpenApiCustomizer);
    }

    /**
     * Customizes the OpenAPI documentation for device endpoints.
     *
     * @param version API version for device documentation
     * @return OpenApiCustomizer for device API
     */
    @Bean
    public OpenApiCustomizer deviceCustomizer(@Value("${swagger.doc.device.version}") String version) {
        return openApi -> openApi.info(new Info()
                .title("COSMOS Device API Documentation")
                .description("COSMOS Device resources documentation")
                .version(version));
    }

    /**
     * Defines the OpenAPI group for management-related endpoints.
     *
     * @param securityOpenApiCustomizer customizer for security schemes
     * @param managementTagsCustomizer  customizer for management API tags
     * @param managementInfoCustomizer  customizer for management API info
     * @return GroupedOpenApi for management endpoints
     */
    @Bean
    public GroupedOpenApi managementApi(
            @Qualifier("securityOpenApiCustomizer") OpenApiCustomizer securityOpenApiCustomizer,
            @Qualifier("managementTagsCustomizer") OpenApiCustomizer managementTagsCustomizer,
            @Qualifier("managementInfoCustomizer") OpenApiCustomizer managementInfoCustomizer) {
        return buildGroupedOpenApi("management",
                new String[]{"/management/**", "/api/v1/**"},
                new String[]{"/management/v1/ecuCertificate/**"},
                managementInfoCustomizer, managementTagsCustomizer, securityOpenApiCustomizer);
    }

    /**
     * Customizes the OpenAPI info for management endpoints.
     *
     * @param version API version for management documentation
     * @return OpenApiCustomizer for management API
     */
    @Bean
    public OpenApiCustomizer managementInfoCustomizer(@Value("${swagger.doc.management.version}") String version) {
        return openApi -> openApi.info(new Info()
                .title("COSMOS Management API Documentation")
                .description("COSMOS Management resources documentation")
                .version(version));
    }

    /**
     * Customizes the OpenAPI tags for management endpoints.
     *
     * @return OpenApiCustomizer for management API tags
     */
    @Bean
    public OpenApiCustomizer managementTagsCustomizer() {
        return openApi -> {
            openApi.addTagsItem(new Tag()
                    .name(SwaggerConstants.ACTIONS)
                    .description(SwaggerConstants.ACTIONS_DESCRIPTION));
            Arrays.stream(MGMT_TAGS)
                    .sorted(Comparator.comparing(Tag::getName))
                    .forEach(openApi::addTagsItem);
        };
    }

    /**
     * Defines the OpenAPI group for system-related endpoints.
     *
     * @param systemCustomizer          customizer for system API documentation
     * @param securityOpenApiCustomizer customizer for security schemes
     * @return GroupedOpenApi for system endpoints
     */
    @Bean
    public GroupedOpenApi systemApi(
            @Qualifier("systemCustomizer") OpenApiCustomizer systemCustomizer,
            @Qualifier("securityOpenApiCustomizer") OpenApiCustomizer securityOpenApiCustomizer) {
        return buildGroupedOpenApi("system",
                new String[]{"/system/**"},
                new String[]{},
                systemCustomizer, securityOpenApiCustomizer,
                openApi -> openApi.addTagsItem(new Tag()
                        .name(SwaggerConstants.SYSTEM_CONFIGURATION)
                        .description(SwaggerConstants.SYSTEM_CONFIGURATION_DESCRIPTION)));
    }

    /**
     * Customizes the OpenAPI documentation for system endpoints.
     *
     * @param version API version for system documentation
     * @return OpenApiCustomizer for system API
     */
    @Bean
    public OpenApiCustomizer systemCustomizer(@Value("${swagger.doc.system.version}") String version) {
        return openApi -> openApi.info(new Info()
                .title("COSMOS System API Documentation")
                .description("COSMOS System resources documentation")
                .version(version));
    }

    /**
     * Customizes the OpenAPI documentation to add OAuth2 security scheme.
     *
     * @param scope    OAuth2 scope
     * @param tokenUri OAuth2 token URI
     * @return OpenApiCustomizer for security configuration
     */
    @Bean
    public OpenApiCustomizer securityOpenApiCustomizer(
            @Value("${swagger.doc.security.oidc.scope}") String scope,
            @Value("${swagger.doc.security.oidc.token-uri}") String tokenUri) {
        return openApi -> {
            openApi.addSecurityItem(new SecurityRequirement().addList("OAuth2"));
            openApi.getComponents().addSecuritySchemes("OAuth2",
                    new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(new OAuthFlows()
                                    .clientCredentials(new OAuthFlow()
                                            .tokenUrl(tokenUri)
                                            .scopes(new Scopes().addString(scope, "Access " + scope)))));
        };
    }

    /**
     * Configures redirects for Swagger UI endpoints.
     *
     * @return WebMvcConfigurer for view controller redirects
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * Adds redirect view controllers for Swagger UI endpoints.
             *
             * @param registry ViewControllerRegistry to add redirects
             */
            @Override
            public void addViewControllers(@NotNull ViewControllerRegistry registry) {
                registry.addRedirectViewController("/swagger/device-ui", "/swagger-ui/index.html?urls.primaryName=device");
                registry.addRedirectViewController("/swagger/management-ui", "/swagger-ui/index.html?urls.primaryName=management");
                registry.addRedirectViewController("/swagger/system-ui", "/swagger-ui/index.html?urls.primaryName=system");
            }

            @Override
            public void addInterceptors(@NotNull InterceptorRegistry registry) {
                registry.addInterceptor(new RequestInterceptor());
            }
        };
    }

    /**
     * Utility method to build a GroupedOpenApi with given group name, paths, and customizers.
     *
     * @param groupName    the group name
     * @param pathsToMatch array of paths to match
     * @param pathsToExclude array of paths to exclude
     * @param customizers  OpenApiCustomizers to apply
     * @return GroupedOpenApi instance
     */
    private GroupedOpenApi buildGroupedOpenApi(String groupName, String[] pathsToMatch, String[] pathsToExclude, OpenApiCustomizer... customizers) {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(groupName)
                .pathsToMatch(pathsToMatch)
                .pathsToExclude(pathsToExclude);
        for (OpenApiCustomizer customizer : customizers) {
            builder.addOpenApiCustomizer(customizer);
        }
        return builder.build();
    }

    /**
     * Tags for management API documentation.
     */
    private static final Tag[] MGMT_TAGS = new Tag[]{
            new Tag().name(SwaggerConstants.BASIC_AUTHENTICATION).description(SwaggerConstants.BASIC_AUTHENTICATION_DESCRIPTION),
            new Tag().name(SwaggerConstants.SOFTWARE_MODULES).description(SwaggerConstants.SOFTWARE_MODULES_DESCRIPTION),
            new Tag().name(SwaggerConstants.DOWNLOAD_ARTIFACT).description(SwaggerConstants.DOWNLOAD_ARTIFACT_DESCRIPTION),
            new Tag().name(SwaggerConstants.ROLLOUTS).description(SwaggerConstants.ROLLOUTS_DESCRIPTION),
            new Tag().name(SwaggerConstants.SOFTWARE_VERSION).description(SwaggerConstants.SOFTWARE_VERSION_DESCRIPTION),
            new Tag().name(SwaggerConstants.TARGETS).description(SwaggerConstants.TARGETS_DESCRIPTION),
            new Tag().name(SwaggerConstants.TENANTS).description(SwaggerConstants.TENANTS_DESCRIPTION),
            new Tag().name(SwaggerConstants.VEHICLE).description(SwaggerConstants.VEHICLE_DESCRIPTION),
            new Tag().name(SwaggerConstants.ECU).description(SwaggerConstants.ECU_DESCRIPTION)
    };
}