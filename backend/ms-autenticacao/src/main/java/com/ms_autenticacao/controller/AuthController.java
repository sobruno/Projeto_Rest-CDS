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