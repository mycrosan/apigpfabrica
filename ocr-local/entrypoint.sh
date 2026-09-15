#!/usr/bin/env bash
set -euo pipefail

PROXY_PID=""
encerrar() {
  if [ -n "$PROXY_PID" ] && kill -0 "$PROXY_PID" 2>/dev/null; then
    kill "$PROXY_PID" 2>/dev/null || true
    wait "$PROXY_PID" 2>/dev/null || true
  fi
}
trap encerrar EXIT INT TERM

# Quando OLLAMA_HOST_REMOTO é informado (ex: container ollama dedicado na mesma rede
# interna), encaminha conexões de loopback do processo Python para o host remoto.
# O TransporteOllamaLocal rejeita explicitamente nomes DNS; manter 127.0.0.1 é
# obrigatório para o contrato de segurança do motor de visão.
if [ -n "${OLLAMA_HOST_REMOTO:-}" ]; then
  : "${OLLAMA_HOST_REMOTO_PORTA:=11434}"
  socat TCP-LISTEN:11434,bind=127.0.0.1,reuseaddr,fork \
        "TCP:${OLLAMA_HOST_REMOTO}:${OLLAMA_HOST_REMOTO_PORTA}" &
  PROXY_PID=$!
  sleep 0.2
fi

exec uvicorn pneus_ocr.app:app --host 0.0.0.0 --port 8091 --workers 1
