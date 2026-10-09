package com.ms_financeiro.controller;

import com.ms_financeiro.dto.ContaDto;
import com.ms_financeiro.model.Conta;
import com.ms_financeiro.model.Transacao;
import com.ms_financeiro.repository.ContaRepository;
import com.ms_financeiro.repository.TransacaoRepository;
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
public class ContaController {

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    private Conta obterOuCriarConta(Long usuarioId) {
        return contaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Conta novaConta = new Conta();
                    novaConta.setUsuarioId(usuarioId);
                    novaConta.setTitular("Usuario #" + usuarioId);
                    novaConta.setSaldo(0.0);
                    return contaRepository.save(novaConta);
                });
    }

    @GetMapping("/minha-conta")
    public ResponseEntity<?> obterMinhaConta(@RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        Conta conta = obterOuCriarConta(usuarioId);
        ContaDto dto = new ContaDto(conta.getId(), conta.getUsuarioId(), conta.getTitular(), conta.getSaldo());

        EntityModel<ContaDto> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(ContaController.class).obterMinhaConta(usuarioId)).withSelfRel());
        resource.add(linkTo(methodOn(ContaController.class).depositar(usuarioId, null)).withRel("depositar"));
        resource.add(linkTo(methodOn(ContaController.class).transferir(usuarioId, null)).withRel("transferir"));
        resource.add(linkTo(methodOn(ContaController.class).obterExtrato(conta.getId())).withRel("extrato"));

        return ResponseEntity.ok(resource);
    }

    @PostMapping("/depositar")
    @Transactional
    public ResponseEntity<?> depositar(@RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
                                       @RequestBody Map<String, Object> payload) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        Double valor = Double.parseDouble(payload.get("valor").toString());
        if (valor <= 0) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor do depósito deve ser maior que zero"));
        }

        Conta conta = obterOuCriarConta(usuarioId);
        conta.setSaldo(conta.getSaldo() + valor);
        Conta contaSalva = contaRepository.save(conta);

        Transacao transacao = new Transacao();
        transacao.setTipo("DEPOSITO");
        transacao.setContaDestinoId(conta.getId());
        transacao.setValor(valor);
        transacao.setDataHora(LocalDateTime.now());
        transacaoRepository.save(transacao);

        ContaDto dto = new ContaDto(contaSalva.getId(), contaSalva.getUsuarioId(), contaSalva.getTitular(), contaSalva.getSaldo());

        EntityModel<ContaDto> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(ContaController.class).obterMinhaConta(usuarioId)).withRel("minha-conta"));
        resource.add(linkTo(methodOn(ContaController.class).depositar(usuarioId, null)).withSelfRel());
        resource.add(linkTo(methodOn(ContaController.class).transferir(usuarioId, null)).withRel("transferir"));
        resource.add(linkTo(methodOn(ContaController.class).obterExtrato(contaSalva.getId())).withRel("extrato"));

        return ResponseEntity.ok(resource);
    }

    @PostMapping("/transferir")
    @Transactional
    public ResponseEntity<?> transferir(@RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
                                        @RequestBody Map<String, Object> payload) {
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Cabeçalho X-User-Id ausente ou inválido"));
        }

        Long contaDestinoId = Long.parseLong(payload.get("contaDestinoId").toString());
        Double valor = Double.parseDouble(payload.get("valor").toString());

        if (valor <= 0) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor da transferência deve ser maior que zero"));
        }

        Conta origem = obterOuCriarConta(usuarioId);
        if (origem.getSaldo() < valor) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Saldo insuficiente para transferência"));
        }

        Conta destino = contaRepository.findById(contaDestinoId).orElse(null);
        if (destino == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Conta de destino não encontrada"));
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

        ContaDto dto = new ContaDto(origem.getId(), origem.getUsuarioId(), origem.getTitular(), origem.getSaldo());

        EntityModel<ContaDto> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(ContaController.class).obterMinhaConta(usuarioId)).withRel("minha-conta"));
        resource.add(linkTo(methodOn(ContaController.class).transferir(usuarioId, null)).withSelfRel());
        resource.add(linkTo(methodOn(ContaController.class).obterExtrato(origem.getId())).withRel("extrato"));

        return ResponseEntity.ok(resource);
    }

    @GetMapping("/{contaId}/extrato")
    public ResponseEntity<List<Transacao>> obterExtrato(@PathVariable Long contaId) {
        List<Transacao> extrato = transacaoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId);
        return ResponseEntity.ok(extrato);
    }
}