# CDS — Carteira Digital Simplificada
##### Projeto executado em parceria com [Henze Fernandes](https://github.com/enzenoide)

Aplicação de demonstração de uma carteira digital. O projeto reúne uma interface web, um API Gateway e dois microsserviços Spring Boot para cadastro/autenticação e operações financeiras.
> Este projeto é educacional e usa bancos H2 em memória. Não o utilize para movimentar dinheiro real nem como sistema financeiro de produção.

## Funcionalidades

- Cadastro de usuário e login com emissão de token JWT.
- Consulta de conta e saldo.
- Depósitos e transferências entre contas.
- Extrato de transações.
- Cadastro e consulta de endereços de destinatários.
- Documentação interativa das APIs via Swagger UI.

## Arquitetura

| Componente | Responsabilidade | Porta local |
| --- | --- | ---: |
| `frontend` | Páginas estáticas servidas pelo Nginx | 3000 |
| `api-gateway` | Roteamento das requisições e validação JWT | 8080 |
| `ms-autenticacao` | Cadastro, login e emissão de JWT | 8081 |
| `ms-financeiro` | Contas, depósitos, transferências e extrato | 8082 |

O navegador acessa a API pelo Gateway em `http://localhost:8080/api`. O Gateway encaminha as rotas `/auth/**` ao serviço de autenticação e as demais rotas ao serviço financeiro. Os dois microsserviços usam bancos H2 em memória independentes, inicializados com dados de demonstração.

## Requisitos

- Docker com Docker Compose v2.
- Para executar os serviços sem Docker: Java 17.

## Executar com Docker Compose

Na raiz do repositório, inicie os quatro serviços:

```bash
docker compose up --build
```

Quando os serviços estiverem prontos, acesse:

- Aplicação: [http://localhost:3000](http://localhost:3000)
- Swagger UI do Gateway: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Documentação OpenAPI consolidada:
  - Autenticação: [http://localhost:8080/docs/autenticacao](http://localhost:8080/docs/autenticacao)
  - Financeiro: [http://localhost:8080/docs/financeiro](http://localhost:8080/docs/financeiro)

Para encerrar os serviços, use `Ctrl+C`. Para remover os containers da execução:

```bash
docker compose down
```

Os dados dos bancos H2 são temporários e são perdidos quando o respectivo serviço é reiniciado.

## Acessar a aplicação

Na interface, crie um usuário em **Cadastre-se** ou use um dos usuários de demonstração carregados pelo serviço de autenticação:

| Usuário | Senha |
| --- | --- |
| João Silva | `senha123` |
| Maria Joaquina Dantas Souza | `senha123` |
| Carol | `senha123` |

Essas credenciais são somente para desenvolvimento local. O cadastro exige nome, endereço e senha; o e-mail é opcional na API. O login aceita nome de usuário ou e-mail.

## API

As rotas abaixo são acessadas pelo Gateway, prefixadas por `/api`. As operações financeiras e de endereços exigem o cabeçalho `Authorization` com o token JWT retornado no login.

| Método | Rota | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/auth/cadastrar` | Público | Cadastra usuário (`nome`, `endereco`, `senha`; `email` opcional). |
| `POST` | `/api/auth/login` | Público | Autentica com `nome` ou e-mail e `senha`; retorna o JWT. |
| `GET` | `/api/contas/minha-conta` | JWT | Consulta ou cria a conta do usuário autenticado. |
| `POST` | `/api/contas/depositar` | JWT | Deposita um valor positivo (`valor`). |
| `POST` | `/api/contas/transferir` | JWT | Transfere um valor positivo para outro destinatário. |
| `GET` | `/api/contas/{contaId}/extrato` | JWT | Consulta transações de entrada e saída da conta. |
| `GET` | `/api/enderecos` | JWT | Lista destinatários salvos pelo usuário. |
| `POST` | `/api/enderecos` | JWT | Salva um destinatário (`nome`, `endereco`). |

O destinatário de uma transferência pode ser informado como chave, nome do titular ou identificador da conta/usuário.

### Exemplos

Cadastrar um usuário:

```bash
curl -X POST http://localhost:8080/api/auth/cadastrar \
  -H "Content-Type: application/json" \
  -d "{\"nome\":\"Ana Lima\",\"endereco\":\"ana-chave\",\"email\":\"ana@example.com\",\"senha\":\"Senha123!\"}"
```

Fazer login e copiar o valor de `token` retornado:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"nome\":\"Ana Lima\",\"senha\":\"Senha123!\"}"
```

Nos exemplos seguintes, substitua `$TOKEN` pelo token retornado no login:

```bash
curl http://localhost:8080/api/contas/minha-conta \
  -H "Authorization: Bearer $TOKEN"
```

Depositar:

```bash
curl -X POST http://localhost:8080/api/contas/depositar \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"valor\":100.00}"
```

Transferir para uma chave ou destinatário:

```bash
curl -X POST http://localhost:8080/api/contas/transferir \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"destinatario\":\"chave-do-destino\",\"valor\":25.00}"
```

## Executar os serviços sem Docker

Abra um terminal por serviço e execute, a partir da pasta correspondente:

```powershell
.\mvnw.cmd spring-boot:run
```

Inicie `ms-autenticacao` (`backend/ms-autenticacao`) e `ms-financeiro` (`backend/ms-financeiro`) antes de `api-gateway` (`backend/api-gateway`). As URLs padrão do Gateway apontam para `http://localhost:8081` e `http://localhost:8082`. Para a interface web, sirva os arquivos da pasta `frontend` em um servidor estático; o frontend espera que o Gateway esteja disponível na porta `8080` do mesmo host.

## Configuração

O `docker-compose.yml` configura a variável `JWT_SECRET` para desenvolvimento e o Gateway usa `MS_AUTENTICACAO_URL` e `MS_FINANCEIRO_URL` para localizar os microsserviços. Fora do Docker, o Gateway usa por padrão as URLs locais definidas em `application.properties`.

Defina um segredo JWT próprio e seguro em qualquer ambiente que não seja local. O valor incluído na configuração do Compose é apenas de demonstração e não deve ser reutilizado em produção.

## Estrutura do repositório

```text
backend/
  api-gateway/       # Gateway, proxy e validação JWT
  ms-autenticacao/   # Cadastro, login e usuários
  ms-financeiro/     # Contas, endereços e transações
frontend/            # Interface HTML, CSS e JavaScript
docker-compose.yml   # Inicialização local dos serviços
```
