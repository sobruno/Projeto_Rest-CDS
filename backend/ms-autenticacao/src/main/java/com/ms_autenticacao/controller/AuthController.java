package com.ms_autenticacao.controller;

import com.ms_autenticacao.model.Usuario;
import com.ms_autenticacao.repository.UsuarioRepository;
import com.ms_autenticacao.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody Usuario body) {
        try {
            // 1. Validação de campos obrigatórios
            if (body.getNome() == null || body.getNome().isBlank() ||
                body.getSenha() == null || body.getSenha().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Preencha todos os campos obrigatórios"));
            }

            // 2. Verifica se o e-mail já existe
            if (usuarioRepository.findByNome(body.getNome()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Nome já cadastrado"));
            }

            // 3. Criptografa a senha e salva
            body.setSenha(passwordEncoder.encode(body.getSenha()));
            Usuario saved = usuarioRepository.save(body);

            // Evita retornar o hash da senha no JSON de resposta por segurança
            saved.setSenha(null);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao cadastrar usuário: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String nome = body.get("nome");
            String senha = body.get("senha");

            if (nome == null || nome.isBlank() || senha == null || senha.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "nome e senha são obrigatórios"));
            }

            var opt = usuarioRepository.findByNome(nome);
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