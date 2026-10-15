package com.ms_financeiro.controller;

import com.ms_financeiro.model.Address;
import com.ms_financeiro.repository.AddressRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enderecos")
@Tag(name = "Enderecos", description = "Gerenciamento de destinatarios salvos pelo usuario autenticado.")
@SecurityRequirement(name = "bearerAuth")
public class EnderecoController {

    @Autowired
    private AddressRepository addressRepository;

    @GetMapping
    @Operation(summary = "Listar enderecos", description = "Lista os destinatarios salvos pelo usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<?> listar(@Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }
        List<Address> enderecos = addressRepository.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(enderecos);
    }

    @PostMapping
        @Operation(summary = "Salvar endereco", description = "Salva um destinatario para facilitar futuras transferencias.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"nome\":\"Joao Silva\",\"endereco\":\"chave-do-joao\"}"))))
        @ApiResponse(responseCode = "200", description = "Endereco salvo")
        @ApiResponse(responseCode = "400", description = "Nome ou endereco ausente")
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
