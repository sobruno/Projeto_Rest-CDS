package com.ms_autenticacao.controller;

import com.ms_autenticacao.model.Usuario;
import com.ms_autenticacao.repository.UsuarioRepository;
import com.ms_autenticacao.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Cadastro de usuarios e login. Essas operacoes nao exigem token.")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/cadastrar")
        @Operation(summary = "Cadastrar usuario", description = "Cria um usuario. O nome, endereco e senha sao obrigatorios; email e opcional.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"nome\":\"Ana Lima\",\"endereco\":\"ana-chave\",\"email\":\"ana@example.com\",\"senha\":\"Senha123!\"}"))))
        @ApiResponse(responseCode = "200", description = "Usuario cadastrado")
        @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou nome ja cadastrado")
    public ResponseEntity<?> cadastrar(@RequestBody Usuario body) {
        try {
            // 1. Validação de campos obrigatórios
            String nome = body.getNome() != null ? body.getNome().trim() : null;
            String endereco = body.getEndereco() != null ? body.getEndereco().trim() : null;
            String senha = body.getSenha();

            if (nome == null || nome.isBlank() ||
                endereco == null || endereco.isBlank() ||
                senha == null || senha.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Preencha todos os campos obrigatórios"));
            }

            // 2. Verifica se o usuário já existe
            if (usuarioRepository.findByNomeIgnoreCase(nome).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Nome já cadastrado"));
            }

            // 3. Criptografa a senha e salva
            body.setNome(nome);
            body.setEndereco(endereco);
            body.setSenha(passwordEncoder.encode(senha));
            Usuario saved = usuarioRepository.save(body);

            // Evita retornar o hash da senha no JSON de resposta por segurança
            saved.setSenha(null);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao cadastrar usuário: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
        @Operation(summary = "Iniciar sessao", description = "Aceita nome de usuario ou email. Retorna um JWT Bearer para as operacoes financeiras.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"nome\":\"Ana Lima\",\"senha\":\"Senha123!\"}"))))
        @ApiResponse(responseCode = "200", description = "Login realizado; use o campo token no esquema Bearer JWT")
        @ApiResponse(responseCode = "401", description = "Nome/email ou senha invalidos")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String nome = body.get("nome") != null ? body.get("nome").trim() : "";
            String senha = body.get("senha") != null ? body.get("senha") : "";

            if (nome.isBlank() || senha.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Nome de usuário e senha são obrigatórios"));
            }

            var opt = usuarioRepository.findByNomeIgnoreCase(nome)
                    .or(() -> usuarioRepository.findByEmailIgnoreCase(nome));
            if (opt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("erro", "Credenciais inválidas"));
            }

            Usuario u = opt.get();
            if (!passwordEncoder.matches(senha, u.getSenha())) {
                return ResponseEntity.status(401).body(Map.of("erro", "Credenciais inválidas"));
            }

            String token = jwtUtil.gerarToken(u.getId(), u.getNome(), u.getEndereco());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "tipo", "Bearer",
                    "usuarioId", u.getId(),
                    "nome", u.getNome()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao realizar login: " + e.getMessage()));
        }
    }
}