#!/usr/bin/env bash
# =====================================================================
# SVIS - Prueba de Estrés Concurrente en Paralelo (Bash / cURL)
# =====================================================================

BASE_URL="http://localhost:8080/svis/api"
ENCUESTA_ID=${1:-1}

echo "=== SVIS: PRUEBA DE CONCURRENCIA EN PARALELO ==="

# 1. Obtener un token DISPONIBLE
TOKEN_JSON=$(curl -s "$BASE_URL/tokens?encuestaId=$ENCUESTA_ID")
TOKEN=$(echo "$TOKEN_JSON" | grep -o '"estado":"DISPONIBLE"[^}]*' -m 1 | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "No se encontró token DISPONIBLE en la encuesta $ENCUESTA_ID."
    exit 1
fi

echo "[Paso 1] Token seleccionado: $TOKEN"

PAYLOAD="{\"encuesta_id\": $ENCUESTA_ID, \"opcion_id\": 1, \"token\": \"$TOKEN\"}"

echo "[Paso 2] Disparando 2 peticiones paralelas simultáneas..."

RESP1_FILE=$(mktemp)
RESP2_FILE=$(mktemp)

# Disparo asíncrono simultáneo en segundo plano (&)
curl -s -i -X POST "$BASE_URL/votos/emitir" \
     -H "Content-Type: application/json" \
     -d "$PAYLOAD" > "$RESP1_FILE" &
PID1=$!

curl -s -i -X POST "$BASE_URL/votos/emitir" \
     -H "Content-Type: application/json" \
     -d "$PAYLOAD" > "$RESP2_FILE" &
PID2=$!

wait $PID1 $PID2

CODE1=$(head -n 1 "$RESP1_FILE" | awk '{print $2}')
CODE2=$(head -n 1 "$RESP2_FILE" | awk '{print $2}')

echo "----------------------------------------------------"
echo "Petición 1: HTTP $CODE1"
cat "$RESP1_FILE" | tail -n 1
echo ""
echo "----------------------------------------------------"
echo "Petición 2: HTTP $CODE2"
cat "$RESP2_FILE" | tail -n 1
echo ""
echo "----------------------------------------------------"

rm -f "$RESP1_FILE" "$RESP2_FILE"

if { [ "$CODE1" = "200" ] && [ "$CODE2" = "409" ]; } || { [ "$CODE1" = "409" ] && [ "$CODE2" = "200" ]; }; then
    echo ">>> [APROBADO]: 1 petición 200 OK y 1 petición 409 CONFLICT <<<"
    exit 0
else
    echo ">>> [NO APROBADO]: Códigos recibidos ($CODE1, $CODE2) <<<"
    exit 1
fi
