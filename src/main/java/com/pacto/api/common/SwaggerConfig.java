package com.pacto.api.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 설정 (프론트엔드가 Swagger에서 토큰을 넣고 테스트할 수 있게 해줌)
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(new Info()
                        .title("Pacto 프로젝트 API 명세서")
                        .description("B2B2C 에스크로 정산 플랫폼 Pacto의 API 명세입니다.")
                        .version("1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}