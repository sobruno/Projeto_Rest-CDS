package com.ms_financeiro.controller;

import com.ms_financeiro.model.Address;
import com.ms_financeiro.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enderecos")
public class EnderecoController {

    @Autowired
    private AddressRepository addressRepository;

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }
        List<Address> enderecos = addressRepository.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(enderecos);
    }

    @PostMapping
    public ResponseEntity<?> salvar(@RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
                                    @RequestBody Map<String, String> body) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        String nome = body.get("nome");
        String endereco = body.get("endereco");

        if (nome == null || nome.isBlank() || endereco == null || endereco.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nome e endereço são obrigatórios"));
        }

        Address novo = new Address(usuarioId, nome.trim(), endereco.trim());
        Address salvo = addressRepository.save(novo);
        return ResponseEntity.ok(salvo);
    }
}
