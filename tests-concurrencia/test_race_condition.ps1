param (
    [string]$BaseUrl = "http://localhost:8080/svis/api",
    [int]$EncuestaId = 1,
    [int]$UsuarioId = 4
)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  SVIS - PRUEBA DE CONCURRENCIA Y CONDICION DE CARRERA (EVALUACION 5.1)" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Crear una encuesta y padron dedicados para la prueba de estrés en vivo
Write-Host "`n[Paso 1] Creando encuesta y padrón frescos para la prueba de estrés..." -ForegroundColor Yellow

$timestampPrueba = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
$nuevaEnc = @{
    titulo = "Sustentacion Concurrencia - $timestampPrueba"
    descripcion = "Prueba de estres de doble voto simultaneo en paralelo"
    opciones = @("Candidato Concurrencia Alfa", "Candidato Concurrencia Beta", "Voto en Blanco")
} | ConvertTo-Json

$resNueva = Invoke-RestMethod -Uri "$BaseUrl/encuestas" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($nuevaEnc)) -ContentType "application/json; charset=utf-8"
$EncuestaId = $resNueva.id

# Activar encuesta
$bodyActivar = @{ id = $EncuestaId; estado = "ACTIVA" } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/encuestas/estado" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($bodyActivar)) -ContentType "application/json; charset=utf-8" | Out-Null

# Generar tokens con TTL de 180 minutos a partir de este instante
$bodyGen = @{ encuesta_id = $EncuestaId; ttl_minutos = 180 } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/tokens/generar" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($bodyGen)) -ContentType "application/json; charset=utf-8" | Out-Null

$tokensNuevos = Invoke-RestMethod -Uri "$BaseUrl/tokens?encuestaId=$EncuestaId" -Method Get
$token = $tokensNuevos[0].token
Write-Host "Encuesta #$EncuestaId activada con éxito." -ForegroundColor Green
Write-Host "Token OTP fresco asignado para la prueba: $token (Aprendiz: $($tokensNuevos[0].usuarioNombre))" -ForegroundColor Green

# Obtener las opciones reales creadas para la nueva encuesta
$encuestasActivas = Invoke-RestMethod -Uri "$BaseUrl/encuestas/activas" -Method Get
$encTarget = $encuestasActivas | Where-Object { $_.id -eq $EncuestaId } | Select-Object -First 1
$opcionId = $encTarget.opciones[0].id

Write-Host "Opción seleccionada para sufragio ID: $opcionId ($($encTarget.opciones[0].textoOpcion))" -ForegroundColor Cyan

# 2. Preparar el payload de votacion
$bodyVoto = @{
    encuesta_id = $EncuestaId
    opcion_id = $opcionId
    token = $token
} | ConvertTo-Json -Compress

Write-Host "`n[Paso 2] Disparando DOS peticiones HTTP concurrentes en paralelo con el mismo OTP..." -ForegroundColor Yellow
Write-Host "URL Endpoint : $BaseUrl/votos/emitir" -ForegroundColor Gray
Write-Host "Token OTP    : $token" -ForegroundColor Gray

$scriptBlock = {
    param ($url, $payload, $id)
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
        $req = [System.Net.HttpWebRequest]::Create($url)
        $req.Method = "POST"
        $req.ContentType = "application/json; charset=utf-8"
        $req.Timeout = 10000

        $stream = $req.GetRequestStream()
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Close()

        $resp = $req.GetResponse()
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $content = $reader.ReadToEnd()
        $reader.Close()
        $resp.Close()

        return @{
            Peticion = $id
            StatusCode = 200
            StatusDesc = "OK"
            Response = $content
        }
    } catch [System.Net.WebException] {
        $webEx = $_.Exception
        $code = 0
        $desc = "Error"
        $content = ""
        if ($webEx.Response) {
            $httpResp = [System.Net.HttpWebResponse]$webEx.Response
            $code = [int]$httpResp.StatusCode
            $desc = $httpResp.StatusDescription
            $reader = New-Object System.IO.StreamReader($httpResp.GetResponseStream())
            $content = $reader.ReadToEnd()
            $reader.Close()
        }
        return @{
            Peticion = $id
            StatusCode = $code
            StatusDesc = $desc
            Response = $content
        }
    }
}

$urlTarget = "$BaseUrl/votos/emitir"
$job1 = Start-Job -ScriptBlock $scriptBlock -ArgumentList $urlTarget, $bodyVoto, "Peticion-Simultanea-1"
$job2 = Start-Job -ScriptBlock $scriptBlock -ArgumentList $urlTarget, $bodyVoto, "Peticion-Simultanea-2"

Wait-Job $job1, $job2 | Out-Null

$res1 = Receive-Job $job1
$res2 = Receive-Job $job2

Remove-Job $job1, $job2 -Force

Write-Host "`n[Paso 3] Respuestas Obtenidas del Servidor Backend Java (Tomcat):" -ForegroundColor Yellow
Write-Host "----------------------------------------------------------------------"
$color1 = "Magenta"
if ($res1.StatusCode -eq 200) { $color1 = "Green" }
Write-Host "  $($res1.Peticion): HTTP $($res1.StatusCode) $($res1.StatusDesc)" -ForegroundColor $color1
Write-Host "  Cuerpo JSON: $($res1.Response)" -ForegroundColor Gray

Write-Host "----------------------------------------------------------------------"
$color2 = "Magenta"
if ($res2.StatusCode -eq 200) { $color2 = "Green" }
Write-Host "  $($res2.Peticion): HTTP $($res2.StatusCode) $($res2.StatusDesc)" -ForegroundColor $color2
Write-Host "  Cuerpo JSON: $($res2.Response)" -ForegroundColor Gray
Write-Host "----------------------------------------------------------------------"

# 4. Evaluacion formal
$statusCodes = @($res1.StatusCode, $res2.StatusCode)
$has200 = $statusCodes -contains 200
$has409 = $statusCodes -contains 409

Write-Host "`n[RESULTADO DE LA EVALUACION DEL TALLER]:" -ForegroundColor Cyan
if ($has200 -and $has409) {
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host "  CRITERIO 5.1 APROBADO: EXACTAMENTE 1 PETICION 200 OK Y 1 PETICION 409 CONFLICT" -ForegroundColor Green
    Write-Host "  Mecanismo: Transaccion ACID + Bloqueo Pesimista (SELECT ... FOR UPDATE)" -ForegroundColor Green
    Write-Host "  Estado: INMUTABLE Y SIN CONDICION DE CARRERA" -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Green
} else {
    Write-Host "======================================================================" -ForegroundColor Red
    Write-Host "  CRITERIO NO APROBADO: Codigos obtenidos: $($statusCodes -join ', ')" -ForegroundColor Red
    Write-Host "======================================================================" -ForegroundColor Red
}
