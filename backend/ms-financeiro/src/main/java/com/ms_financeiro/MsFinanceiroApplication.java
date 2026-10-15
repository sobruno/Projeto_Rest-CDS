package com.ms_financeiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(title = "API Financeira", version = "1.0.0",
        description = "Contas, transferencias, extratos e enderecos salvos."),
    servers = @Server(url = "/api"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class MsFinanceiroApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsFinanceiroApplication.class, args);
    }
}

