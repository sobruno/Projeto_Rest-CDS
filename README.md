# Carteira Digital Simplificada (CDS)
## Arquitetura Distribuída com REST, SOAP e WebSocket

Este projeto acadêmico implementa uma arquitetura orientada a serviços distribuída, combinando **REST/JSON**, **SOAP/WSDL** e **WebSocket** para resolver problemas reais de um fluxo financeiro de transferências bancárias.

---

## 🏛 Arquitetura da Solução

```text
[Frontend Web] 
      │ 
      ├── (1. REST JSON) ───────────► [API Gateway :8080]
      │                                    │
      │                                    ├── (2. REST) ────► [ms-autenticacao :8081]
      │                                    │
      │                                    ├── (3. REST) ────► [ms-financeiro :8082]
      │                                    │
      │                                    ├── (4. SOAP XML) ─► [soap-comprovante :8088] (JAX-WS)
      │                                    │                        ▲
      │                                    │                        │ (Interoperabilidade)
      │                                    │               [cliente-soap-python] (Zeep)
      │                                    │
      ◄── (5. Push WebSocket) ─────────────┘
```

### Componentes do Sistema

| Módulo | Tecnologia | Porta | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **frontend** | HTML5, Tailwind CSS, JS Puro | `3000` | Interface do usuário com atualização de saldo em tempo real e exibição de comprovante. |
| **api-gateway** | Spring Boot 3, WebSocket, RestTemplate | `8080` | Ponto de entrada único, validação JWT, proxy transparente, orquestração SOAP e servidor WebSocket (`/ws/notificacoes`). |
| **ms-autenticacao** | Spring Boot 3, Spring Security, JWT | `8081` | Cadastro de usuários, emissão e validação de tokens JWT. |
| **ms-financeiro** | Spring Boot 3, Spring Data JPA, H2 | `8082` | Gestão de contas bancárias, depósitos, transferências e extratos. |
| **soap-comprovante** | Java JAX-WS Nativo (sem Spring) | `8088` | Microsserviço provedor SOAP para emissão de comprovantes oficiais com hash SHA-256 e UUID. |
| **cliente-soap-python**| Python 3, Zeep | - | Cliente consumidor SOAP que comprova interoperabilidade entre tecnologias distintas. |

---

## 🚀 Como Executar o Projeto

### 1. Executando Todos os Serviços via Docker Compose

```bash
docker compose up -d --build
```
Serviços disponíveis:
* **Frontend:** `http://localhost:3000`
* **API Gateway & Swagger:** `http://localhost:8080/swagger-ui/index.html`
* **Serviço SOAP (WSDL):** `http://localhost:8088/ws/comprovante?wsdl`

---

### 2. Executando o Microsserviço SOAP Isoladamente

```bash
cd backend/soap-comprovante
java -jar target/soap-comprovante.jar
```
* O endpoint SOAP estará ativo em: `http://localhost:8088/ws/comprovante`
* O contrato WSDL estará em: `http://localhost:8088/ws/comprovante?wsdl`

---

### 3. Executando o Cliente SOAP Consumidor em Python (Zeep)

Para demonstrar a interoperabilidade entre o servidor Java e o cliente Python:

```bash
cd cliente-soap-python
pip install -r requirements.txt
python cliente.py
```
O script executa automaticamente 4 testes:
1. Chamada válida de emissão de comprovante (`gerarComprovante`);
2. Chamada válida de consulta do comprovante emitido (`consultarComprovante`);
3. Chamada inválida com valor negativo capturando o `SOAP Fault`;
4. Chamada inválida com UUID inexistente capturando o `SOAP Fault`.

---

### 4. Testes e Documentação no SoapUI

Um projeto pronto do SoapUI está disponível em:
`docs/soapui/comprovante-soapui-project.xml`

Basta abrir o SoapUI, selecionar **File -> Import Project** e carregar o arquivo XML para validar as requisições e exportar a documentação do serviço.

