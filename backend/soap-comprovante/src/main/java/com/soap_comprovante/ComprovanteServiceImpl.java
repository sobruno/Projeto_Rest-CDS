package com.soap_comprovante;

import jakarta.jws.WebService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@WebService(
        endpointInterface = "com.soap_comprovante.ComprovanteService",
        targetNamespace = "http://soap.banco.com/",
        serviceName = "ComprovanteService",
        portName = "ComprovantePort"
)
public class ComprovanteServiceImpl implements ComprovanteService {

    private static final Map<String, Comprovante> REPOSITORIO_COMPROVANTES = new ConcurrentHashMap<>();

    @Override
    public Comprovante gerarComprovante(String nomeRemetente, String nomeDestinatario,
                                        String chaveDestinatario, Double valor,
                                        String dataHora) throws ComprovanteFault {
        System.out.println("[SOAP-SERVER] Recebida solicitação de emissão de comprovante:");
        System.out.printf("  -> Remetente: %s | Destinatário: %s | Valor: R$ %s%n", nomeRemetente, nomeDestinatario, valor);

        if (nomeRemetente == null || nomeRemetente.trim().isEmpty() ||
            nomeDestinatario == null || nomeDestinatario.trim().isEmpty()) {
            throw new ComprovanteFault(
                    "Dados cadastrais incompletos para geração do comprovante.",
                    new ComprovanteFaultInfo("VALIDACAO_CAMPOS_OBRIGATORIOS", "Remetente e Destinatário não podem ser vazios.")
            );
        }

        if (valor == null || valor <= 0.0) {
            throw new ComprovanteFault(
                    "Valor da transferência inválido.",
                    new ComprovanteFaultInfo("VALOR_INVALIDO", "O valor a ser registrado no comprovante deve ser estritamente maior que zero.")
            );
        }

        String dataHoraRegistro = (dataHora != null && !dataHora.isBlank())
                ? dataHora
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String codigoComprovante = UUID.randomUUID().toString();
        String autenticacaoEletronica = gerarHashAutenticacao(codigoComprovante, nomeRemetente, nomeDestinatario, valor, dataHoraRegistro);

        Comprovante comprovante = new Comprovante(
                codigoComprovante,
                autenticacaoEletronica,
                nomeRemetente.trim(),
                nomeDestinatario.trim(),
                chaveDestinatario != null ? chaveDestinatario.trim() : "N/D",
                valor,
                dataHoraRegistro,
                "EMITIDO"
        );

        REPOSITORIO_COMPROVANTES.put(codigoComprovante, comprovante);
        System.out.println("[SOAP-SERVER] Comprovante gerado com sucesso: " + codigoComprovante);
        return comprovante;
    }

    @Override
    public Comprovante consultarComprovante(String codigoComprovante) throws ComprovanteFault {
        System.out.println("[SOAP-SERVER] Consulta de comprovante recebida: " + codigoComprovante);

        if (codigoComprovante == null || codigoComprovante.trim().isEmpty()) {
            throw new ComprovanteFault(
                    "Código do comprovante não fornecido.",
                    new ComprovanteFaultInfo("CODIGO_AUSENTE", "Informe o código identificador UUID do comprovante.")
            );
        }

        Comprovante comprovante = REPOSITORIO_COMPROVANTES.get(codigoComprovante.trim());
        if (comprovante == null) {
            throw new ComprovanteFault(
                    "Comprovante não localizado no repositório oficial.",
                    new ComprovanteFaultInfo("NAO_ENCONTRADO", "Nenhum comprovante com o código " + codigoComprovante + " foi encontrado.")
            );
        }

        return comprovante;
    }

    private String gerarHashAutenticacao(String id, String remetente, String destinatario, Double valor, String dataHora) {
        try {
            String raw = id + "|" + remetente + "|" + destinatario + "|" + valor + "|" + dataHora;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02X", hash[i]));
                if (i % 2 == 1 && i < 7) {
                    sb.append(".");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "AUT-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
        }
    }
}
