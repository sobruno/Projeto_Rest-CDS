package com.ms_financeiro.controller;

import com.ms_financeiro.dto.ContaDto;
import com.ms_financeiro.model.Address;
import com.ms_financeiro.model.Conta;
import com.ms_financeiro.model.Transacao;
import com.ms_financeiro.repository.AddressRepository;
import com.ms_financeiro.repository.ContaRepository;
import com.ms_financeiro.repository.TransacaoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/contas")
@Tag(name = "Contas", description = "Consulta de conta, depositos, transferencias e extratos.")
@SecurityRequirement(name = "bearerAuth")
public class ContaController {

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private AddressRepository addressRepository;

    private Conta obterOuCriarConta(Long usuarioId, String titular, String chave) {
        return contaRepository.findByUsuarioId(usuarioId)
                .map(conta -> {
                    boolean alterado = false;
                    if (titular != null && !titular.isBlank() && (conta.getTitular() == null || conta.getTitular().startsWith("Usuario #"))) {
                        conta.setTitular(titular);
                        alterado = true;
                    }
                    if (chave != null && !chave.isBlank() && (conta.getChave() == null || conta.getChave().startsWith("CONTA"))) {
                        conta.setChave(chave);
                        alterado = true;
                    }
                    return alterado ? contaRepository.save(conta) : conta;
                })
                .orElseGet(() -> {
                    Conta novaConta = new Conta();
                    novaConta.setUsuarioId(usuarioId);
                    novaConta.setTitular(titular != null && !titular.isBlank() ? titular : "Usuario #" + usuarioId);
                    novaConta.setChave(chave != null && !chave.isBlank() ? chave : "CONTA" + usuarioId);
                    novaConta.setSaldo(0.0);
                    return contaRepository.save(novaConta);
                });
    }

    @GetMapping("/minha-conta")
        @Operation(summary = "Consultar minha conta", description = "Retorna a conta vinculada ao usuario autenticado.")
        @ApiResponse(responseCode = "200", description = "Conta encontrada")
        @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    public ResponseEntity<?> obterMinhaConta(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Nome", required = false) String userNome,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Chave", required = false) String userChave) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        Conta conta = obterOuCriarConta(usuarioId, userNome, userChave);
        ContaDto dto = new ContaDto(conta.getId(), conta.getUsuarioId(), conta.getTitular(), conta.getChave(), conta.getSaldo());

        EntityModel<ContaDto> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(ContaController.class).obterMinhaConta(usuarioId, userNome, userChave)).withSelfRel());
        resource.add(linkTo(methodOn(ContaController.class).depositar(usuarioId, null)).withRel("depositar"));
        resource.add(linkTo(methodOn(ContaController.class).transferir(usuarioId, userNome, userChave, null)).withRel("transferir"));
        resource.add(linkTo(methodOn(ContaController.class).obterExtrato(conta.getId())).withRel("extrato"));

        return ResponseEntity.ok(resource);
    }

    @PostMapping("/depositar")
    @Transactional
        @Operation(summary = "Depositar", description = "Adiciona um valor positivo ao saldo da conta autenticada.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"valor\":100.00}"))))
        @ApiResponse(responseCode = "200", description = "Deposito realizado")
        @ApiResponse(responseCode = "400", description = "Valor ausente ou menor/igual a zero")
        @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    public ResponseEntity<?> depositar(@RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
                                       @RequestBody Map<String, Object> payload) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        if (payload == null || payload.get("valor") == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor do depósito não informado"));
        }

        Double valor = Double.parseDouble(payload.get("valor").toString());
        if (valor <= 0) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor do depósito deve ser maior que zero"));
        }

        Conta conta = obterOuCriarConta(usuarioId, null, null);
        conta.setSaldo(conta.getSaldo() + valor);
        Conta contaSalva = contaRepository.save(conta);

        Transacao transacao = new Transacao();
        transacao.setTipo("DEPOSITO");
        transacao.setContaDestinoId(conta.getId());
        transacao.setValor(valor);
        transacao.setDataHora(LocalDateTime.now());
        transacaoRepository.save(transacao);

        ContaDto dto = new ContaDto(contaSalva.getId(), contaSalva.getUsuarioId(), contaSalva.getTitular(), contaSalva.getChave(), contaSalva.getSaldo());

        EntityModel<ContaDto> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(ContaController.class).obterMinhaConta(usuarioId, null, null)).withRel("minha-conta"));
        resource.add(linkTo(methodOn(ContaController.class).depositar(usuarioId, null)).withSelfRel());
        resource.add(linkTo(methodOn(ContaController.class).transferir(usuarioId, null, null, null)).withRel("transferir"));
        resource.add(linkTo(methodOn(ContaController.class).obterExtrato(contaSalva.getId())).withRel("extrato"));

        return ResponseEntity.ok(resource);
    }

    @PostMapping("/transferir")
    @Transactional
        @Operation(summary = "Transferir", description = "Transfere um valor positivo para outra conta. O destinatario pode ser uma chave, nome ou identificador de conta.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"destinatario\":\"chave-do-destino\",\"valor\":25.00}"))))
        @ApiResponse(responseCode = "200", description = "Transferencia realizada")
        @ApiResponse(responseCode = "400", description = "Destinatario invalido, saldo insuficiente ou valor invalido")
        @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    public ResponseEntity<?> transferir(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Nome", required = false) String userNome,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Chave", required = false) String userChave,
            @RequestBody Map<String, Object> payload) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Payload de transferência inválido"));
        }

        Object destObj = payload.get("destinatario");
        if (destObj == null || destObj.toString().trim().isEmpty()) {
            destObj = payload.get("contaDestinoId");
        }
        if (destObj == null || destObj.toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Destinatário ou chave de destino não informado"));
        }

        if (payload.get("valor") == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor da transferência não informado"));
        }

        Double valor = Double.parseDouble(payload.get("valor").toString());
        if (valor <= 0) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor da transferência deve ser maior que zero"));
        }

        Conta origem = obterOuCriarConta(usuarioId, userNome, userChave);
        if (origem.getSaldo() < valor) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Saldo insuficiente para transferência"));
        }

        String destStr = destObj.toString().trim();
        Conta destino = buscarContaDestino(destStr);

        if (destino == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Conta ou chave de destino não encontrada: " + destStr));
        }

        if (origem.getId().equals(destino.getId())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Não é possível transferir para a própria conta"));
        }

        origem.setSaldo(origem.getSaldo() - valor);
        destino.setSaldo(destino.getSaldo() + valor);

        contaRepository.save(origem);
        contaRepository.save(destino);

        Transacao transacao = new Transacao();
        transacao.setTipo("TRANSFERENCIA");
        transacao.setContaOrigemId(origem.getId());
        transacao.setContaDestinoId(destino.getId());
        transacao.setValor(valor);
        transacao.setDataHora(LocalDateTime.now());
        transacaoRepository.save(transacao);

        ContaDto dto = new ContaDto(origem.getId(), origem.getUsuarioId(), origem.getTitular(), origem.getChave(), origem.getSaldo());

        EntityModel<ContaDto> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(ContaController.class).obterMinhaConta(usuarioId, userNome, userChave)).withRel("minha-conta"));
        resource.add(linkTo(methodOn(ContaController.class).transferir(usuarioId, userNome, userChave, null)).withSelfRel());
        resource.add(linkTo(methodOn(ContaController.class).obterExtrato(origem.getId())).withRel("extrato"));

        return ResponseEntity.ok(resource);
    }

    private Conta buscarContaDestino(String destStr) {
        // 1. Busca pela chave exata da conta
        Conta conta = contaRepository.findByChaveIgnoreCase(destStr).orElse(null);
        if (conta != null) return conta;

        // 2. Busca pelo nome do titular
        conta = contaRepository.findByTitularIgnoreCase(destStr).orElse(null);
        if (conta != null) return conta;

        // 3. Se for numérico, tenta buscar por id da conta ou usuarioId
        try {
            Long numId = Long.parseLong(destStr);
            conta = contaRepository.findById(numId)
                    .or(() -> contaRepository.findByUsuarioId(numId))
                    .orElse(null);
            if (conta != null) return conta;
        } catch (NumberFormatException ignored) {}

        // 4. Se não achou na conta diretamente, verifica se existe um endereço salvo com essa chave
        Address addr = addressRepository.findByEndereco(destStr);
        if (addr != null) {
            conta = contaRepository.findByChaveIgnoreCase(addr.getEndereco())
                    .or(() -> contaRepository.findByTitularIgnoreCase(addr.getNome()))
                    .orElse(null);
            if (conta != null) return conta;
        }

        return null;
    }

    @GetMapping("/{contaId}/extrato")
    @Operation(summary = "Consultar extrato", description = "Lista as transacoes de entrada e saida de uma conta, da mais recente para a mais antiga.")
    @ApiResponse(responseCode = "200", description = "Extrato retornado")
    public ResponseEntity<List<Transacao>> obterExtrato(@PathVariable Long contaId) {
        List<Transacao> extrato = transacaoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId);
        return ResponseEntity.ok(extrato);
    }
}