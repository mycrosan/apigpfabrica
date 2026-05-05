#!/bin/bash

# Script para testar o erro de número de série duplicado

echo "Testando erro de número de série duplicado..."

# Primeiro, vamos registrar uma máquina
echo "1. Registrando primeira máquina com número de série TEST-001..."
curl -X POST "http://localhost:8080/api/registro-maquina" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "nome": "Máquina Teste 1",
    "descricao": "Primeira máquina de teste",
    "numeroSerie": "TEST-001",
    "status": "ATIVA"
  }' | jq .

echo -e "\n2. Tentando registrar segunda máquina com o mesmo número de série TEST-001..."
curl -X POST "http://localhost:8080/api/registro-maquina" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "nome": "Máquina Teste 2",
    "descricao": "Segunda máquina de teste",
    "numeroSerie": "TEST-001",
    "status": "ATIVA"
  }' | jq .

echo -e "\nTeste concluído!"