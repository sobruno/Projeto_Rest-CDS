package com.ms_autenticacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(title = "API de Autenticacao", version = "1.0.0",
        description = "Cadastro de usuarios e autenticacao para acesso aos servicos."),
    servers = @Server(url = "/api"))
public class MsAutenticacaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAutenticacaoApplication.class, args);
    }
}

