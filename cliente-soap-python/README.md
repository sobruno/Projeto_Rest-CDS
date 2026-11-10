# Cliente Consumidor SOAP em Python (Zeep)

Este cliente foi desenvolvido para comprovar a **interoperabilidade de tecnologias distintas** consumindo o serviço Web SOAP publicado em Java JAX-WS.

## Pré-requisitos
* Python 3.10 ou superior
* Biblioteca `zeep`:
  ```bash
  pip install -r requirements.txt
  ```

## Como Executar
1. Certifique-se de que o servidor SOAP Java está rodando na porta 8088 (`http://localhost:8088/ws/comprovante?wsdl`).
2. Execute o cliente Python:
   ```bash
   python cliente.py
   ```

## Cenários Demonstrados
1. **Chamada bem-sucedida (Operação 1 - `gerarComprovante`)**: Envia dados da transferência bancária e recebe UUID e Hash de autenticação emitidos pelo SOAP.
2. **Chamada bem-sucedida (Operação 2 - `consultarComprovante`)**: Consulta os dados originais a partir do UUID gerado.
3. **Tratamento de Erro 1 (SOAP Fault)**: Envio de valor negativo, capturando o Fault `VALOR_INVALIDO`.
4. **Tratamento de Erro 2 (SOAP Fault)**: Consulta de código inexistente, capturando o Fault `NAO_ENCONTRADO`.

Essas execuções atendem às exigências de prints para o relatório em PDF e para a apresentação prática em sala de aula.
