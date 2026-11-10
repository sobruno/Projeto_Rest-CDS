# Guia do Trabalho Prático: Serviço Web SOAP, Interoperabilidade e SoapUI

Este guia foi elaborado para orientar os testes, a geração de evidências para o relatório em PDF (máximo 4 páginas) e o roteiro da apresentação prática em sala (até 10 minutos).

---

## 1. Visão Geral da Arquitetura

O projeto integra três padrões arquiteturais de comunicação com papéis bem delimitados:

1. **REST / JSON (API Gateway e Microsserviços Internos)**: Roteamento leve e desacoplado entre Frontend web, Autenticação e Gestão de Contas.
2. **SOAP Web Service (JAX-WS Nativo)**: Emissão e validação de comprovantes de transferência bancária com contrato formal estrito (WSDL/XSD), tipagem forte e integridade criptográfica (Hash SHA-256 e UUID).
3. **WebSocket**: Notificação push em tempo real (`/ws/notificacoes`) para atualização instantânea do saldo e extrato tanto para o remetente quanto para o favorecido, sem necessidade de recarregar a página.

---

## 2. Roteiro para o Relatório em PDF (Máximo 4 Páginas)

O edital exige os seguintes 4 prints e justificativa técnica:

### Print 1: Servidor SOAP em Execução
* **Como reproduzir**:
  ```bash
  cd backend/soap-comprovante
  java -jar target/soap-comprovante.jar
  ```
* **O que capturar**: O terminal exibindo o banner de inicialização e a mensagem:
  ```text
  [SOAP-SERVER] Endpoint publicado com sucesso!
  [SOAP-SERVER] URL do Serviço : http://0.0.0.0:8088/ws/comprovante
  [SOAP-SERVER] WSDL disponível: http://0.0.0.0:8088/ws/comprovante?wsdl
  ```

### Print 2: Cliente Consumidor em Tecnologia Distinta (Python + Zeep)
* **Como reproduzir**:
  ```bash
  cd cliente-soap-python
  python cliente.py
  ```
* **O que capturar**: O terminal com a execução do script Python exibindo os 4 cenários (chamadas válidas e captura de SOAP Faults).

### Print 3: Testes no SoapUI (Chamada Válida e Inválida)
* **Como reproduzir**:
  1. Abra o **SoapUI**.
  2. Clique em **File -> Import Project** e selecione o arquivo:
     `docs/soapui/comprovante-soapui-project.xml`
  3. Abra a requisição `gerarComprovante -> 1. Chamada Valida (Sucesso)` e clique no botão verde Play (▶).
  4. Abra a requisição `gerarComprovante -> 2. Chamada Invalida (Valor Negativo)` e clique no Play (▶) para ver o **SOAP Fault**.
* **O que capturar**: A tela dividida do SoapUI exibindo o XML de Request à esquerda e o XML de Response (ou SOAP Fault) à direita.

### Print 4: Exportação da Documentação do Serviço via SoapUI
* **Como reproduzir no SoapUI**:
  1. No painel esquerdo do SoapUI, clique com o botão direito sobre a interface `ComprovantePortBinding`.
  2. Selecione a opção **"Export Documentation"** (ou **"Generate Documentation"** em versões Pro/OpenSource) ou abra a aba **WSDL Content** e exporte o HTML gerado.
* **O que capturar**: Print da página HTML gerada pelo SoapUI com a documentação dos métodos, namespaces e schemas.

---

## 3. Justificativa Técnica para o PDF (Pronta para colar - 9 linhas)

> A adoção de SOAP/WSDL para a emissão de comprovantes é justificada pela necessidade de contrato formal estrito, imutabilidade e garantia de tipagem, alinhando-se aos padrões de auditoria e mensageria bancária (como SPB e CIP). Enquanto o ecossistema interno opera em REST/JSON para agilidade na camada de apresentação, o SOAP garante conformidade documental com envelopes XML fortemente tipados. Como principal desafio técnico, destaca-se a orquestração no API Gateway em Java, que intercepta a conclusão da transação REST, consome o endpoint RPC/SOAP via XML com namespaces, trata potenciais SOAP Faults e converte o comprovante resultante em JSON para exibição síncrona no front-end, sincronizando simultaneamente os saldos dos envolvidos via WebSocket em tempo real.

---

## 4. Roteiro da Apresentação Prática em Sala (Até 10 Minutos)

| Minuto | Etapa | O que mostrar na tela / falar |
| :--- | :--- | :--- |
| **00:00 - 02:00** | **Serviço Ativo e WSDL** | 1. Iniciar o servidor SOAP: `java -jar soap-comprovante.jar`.<br>2. Abrir o navegador em `http://localhost:8088/ws/comprovante?wsdl`.<br>3. Explicar brevemente as tags do WSDL: `types`, `message`, `portType` (com as operações `gerarComprovante` e `consultarComprovante`), `binding` e `service`. |
| **02:00 - 05:00** | **Demonstração no SoapUI** | 1. Executar a chamada válida de `gerarComprovante` mostrando os elementos `Envelope`, `Body`, e a resposta contendo `codigoComprovante` (UUID) e `autenticacaoEletronica` (Hash SHA-256).<br>2. Executar a chamada inválida (valor negativo) mostrando o **`SOAP-ENV:Fault`** com a mensagem de validação negocial. |
| **05:00 - 07:30** | **Interoperabilidade (Cliente Python)** | 1. Executar `python cliente-soap-python/cliente.py`.<br>2. Explicar que o cliente foi feito em Python com a biblioteca **Zeep**, demonstrando a interoperabilidade real entre um provedor Java e consumidor Python via protocolo SOAP padrão. |
| **07:30 - 10:00** | **Fluxo Completo Integrado (Front + Gateway + WS)** | 1. Abrir a aplicação web (`http://localhost:3000`).<br>2. Fazer uma transferência: o Gateway chama o serviço SOAP, abre o **modal com o comprovante oficial**, e o **WebSocket** atualiza o saldo na tela em tempo real sem recarregar a página. |
