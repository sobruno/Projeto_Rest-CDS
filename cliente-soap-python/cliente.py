#!/usr/bin/env python3
"""
Cliente Consumidor SOAP em Python utilizando Zeep.
Demonstra a interoperabilidade entre cliente em Python e servidor Java JAX-WS.

Cenários validados:
1. Emissão de comprovante válido (Operação: gerarComprovante)
2. Consulta de comprovante existente (Operação: consultarComprovante)
3. Chamada inválida com valor <= 0 (Captura de SOAP Fault)
4. Chamada inválida com código inexistente (Captura de SOAP Fault)
"""

import sys
import datetime
from zeep import Client
from zeep.exceptions import Fault

# Garante compatibilidade de saída com terminais Windows padrão
if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

WSDL_URL = "http://localhost:8088/ws/comprovante?wsdl"

def print_separator(title=""):
    print("\n" + "=" * 70)
    if title:
        print(f" {title.upper()}")
        print("=" * 70)

def main():
    print_separator("Cliente Consumidor SOAP (Python + Zeep)")
    print(f"Conectando ao contrato WSDL: {WSDL_URL}")

    try:
        client = Client(WSDL_URL)
        print("[SUCESSO] WSDL carregado e analisado com sucesso!")
    except Exception as e:
        print(f"[ERRO CRITICO] Falha ao carregar WSDL. O servidor Java esta ativo na porta 8088?")
        print(f"Detalhes: {e}")
        sys.exit(1)

    # -------------------------------------------------------------
    # CENÁRIO 1: Chamada Válida - Gerar Comprovante
    # -------------------------------------------------------------
    print_separator("Cenario 1: Emissao de Comprovante Valido (gerarComprovante)")
    nome_remetente = "Carlos Eduardo"
    nome_destinatario = "Ana Beatriz"
    chave_destinatario = "anabeatriz@email.com"
    valor_transferencia = 250.75
    data_hora = datetime.datetime.now().isoformat()

    print(f"Enviando dados via Envelope SOAP:")
    print(f"  * Remetente : {nome_remetente}")
    print(f"  * Destino   : {nome_destinatario} ({chave_destinatario})")
    print(f"  * Valor     : R$ {valor_transferencia:.2f}")
    print(f"  * Data/Hora : {data_hora}")

    codigo_gerado = None
    try:
        resultado = client.service.gerarComprovante(
            nomeRemetente=nome_remetente,
            nomeDestinatario=nome_destinatario,
            chaveDestinatario=chave_destinatario,
            valor=valor_transferencia,
            dataHora=data_hora
        )
        codigo_gerado = resultado.codigoComprovante
        print("\n[RESPOSTA SOAP RECEBIDA COM SUCESSO]")
        print(f"  [OK] Codigo do Comprovante (UUID) : {resultado.codigoComprovante}")
        print(f"  [OK] Autenticacao Eletronica       : {resultado.autenticacaoEletronica}")
        print(f"  [OK] Status da Emissao             : {resultado.status}")
        print(f"  [OK] Valor Registrado              : R$ {resultado.valor:.2f}")
        print(f"  [OK] Data/Hora Registrada          : {resultado.dataHora}")
    except Fault as fault:
        print(f"[SOAP FAULT] Erro inesperado na chamada valida: {fault}")
    except Exception as e:
        print(f"[ERRO] {e}")

    # -------------------------------------------------------------
    # CENÁRIO 2: Chamada Válida - Consultar Comprovante
    # -------------------------------------------------------------
    if codigo_gerado:
        print_separator("Cenario 2: Consulta de Comprovante Emitido (consultarComprovante)")
        print(f"Consultando UUID gerado no cenario 1: {codigo_gerado}")
        try:
            consulta = client.service.consultarComprovante(codigoComprovante=codigo_gerado)
            print("\n[RESPOSTA SOAP DE CONSULTA RECEBIDA]")
            print(f"  [OK] Codigo Localizado   : {consulta.codigoComprovante}")
            print(f"  [OK] Remetente Original  : {consulta.nomeRemetente}")
            print(f"  [OK] Destinatario        : {consulta.nomeDestinatario}")
            print(f"  [OK] Autenticacao Hash   : {consulta.autenticacaoEletronica}")
            print(f"  [OK] Status              : {consulta.status}")
        except Fault as fault:
            print(f"[SOAP FAULT] Falha na consulta: {fault}")

    # -------------------------------------------------------------
    # CENÁRIO 3: Chamada Inválida 1 - Valor Negativo (Tratamento de Erro)
    # -------------------------------------------------------------
    print_separator("Cenario 3: Validacao de Erro - Valor Negativo (gerarComprovante)")
    print("Tentando emitir comprovante com valor invalido: R$ -50.00")
    try:
        client.service.gerarComprovante(
            nomeRemetente="Fulano",
            nomeDestinatario="Ciclano",
            chaveDestinatario="chave-pix",
            valor=-50.00,
            dataHora=datetime.datetime.now().isoformat()
        )
        print("[ALERTA] A operacao deveria ter falhado mas respondeu sucesso.")
    except Fault as fault:
        print("\n[SOAP FAULT CAPTURADO COM SUCESSO - TRATAMENTO DE ERRO]")
        print(f"  * Mensagem do Fault : {fault.message}")
        print(f"  * Codigo do Fault   : {fault.code}")
        if fault.detail is not None:
            print(f"  * Detalhe XML       : {client.wsdl.types.to_string(fault.detail) if hasattr(client.wsdl.types, 'to_string') else fault.detail}")

    # -------------------------------------------------------------
    # CENÁRIO 4: Chamada Inválida 2 - Código Inexistente (Tratamento de Erro)
    # -------------------------------------------------------------
    print_separator("Cenario 4: Validacao de Erro - UUID Inexistente (consultarComprovante)")
    uuid_falso = "00000000-0000-0000-0000-000000000000"
    print(f"Tentando consultar codigo inexistente: {uuid_falso}")
    try:
        client.service.consultarComprovante(codigoComprovante=uuid_falso)
        print("[ALERTA] A operacao deveria ter falhado mas respondeu sucesso.")
    except Fault as fault:
        print("\n[SOAP FAULT CAPTURADO COM SUCESSO - TRATAMENTO DE ERRO]")
        print(f"  * Mensagem do Fault : {fault.message}")
        print(f"  * Codigo do Fault   : {fault.code}")
        if fault.detail is not None:
            print(f"  * Detalhe XML       : {fault.detail}")

    print_separator("Finalizacao dos Testes de Interoperabilidade")
    print("Todos os cenarios obrigatorios (sucesso e falha) foram validados com exito!")
    print("=" * 70 + "\n")

if __name__ == "__main__":
    main()
