# Microsserviço SOAP - Emissão e Consulta de Comprovantes Bancários

Micro-serviço Web SOAP implementado em **Java JAX-WS nativo** (`jakarta.xml.ws`), sem o uso de abstrações mágicas ou geradores automáticos que ocultem os envelopes e o contrato.

## Endpoints e Contrato
* **Endpoint do Serviço:** `http://localhost:8088/ws/comprovante`
* **Contrato WSDL:** `http://localhost:8088/ws/comprovante?wsdl`
* **Schema XSD:** `http://localhost:8088/ws/comprovante?xsd=1`

## Operações Expostas
1. **`gerarComprovante`**:
   * **Parâmetros de Entrada:**
     * `nomeRemetente` (string, obrigatório)
     * `nomeDestinatario` (string, obrigatório)
     * `chaveDestinatario` (string, opcional/chave de destino)
     * `valor` (double, obrigatório, maior que zero)
     * `dataHora` (string no formato ISO-8601)
   * **Retorno:** Objeto `Comprovante` com UUID (`codigoComprovante`), hash SHA-256 (`autenticacaoEletronica`), status `"EMITIDO"` e dados repassados.
   * **Tratamento de Erro:** Lança `ComprovanteFault` (SOAP Fault) se os campos obrigatórios estiverem ausentes ou se o valor for menor/igual a zero.

2. **`consultarComprovante`**:
   * **Parâmetros de Entrada:**
     * `codigoComprovante` (string, UUID do comprovante)
   * **Retorno:** Objeto `Comprovante` armazenado em memória.
   * **Tratamento de Erro:** Lança `ComprovanteFault` caso o código não seja localizado no repositório.

## Como Executar

### Opção 1: Via JAR empacotado (Recomendado)
```bash
java -jar target/soap-comprovante.jar
```

### Opção 2: Via Maven Wrapper
```bash
.\mvnw.cmd compile exec:java
```

### Opção 3: Via Docker
```bash
docker build -t soap-comprovante .
docker run -p 8088:8088 soap-comprovante
```
