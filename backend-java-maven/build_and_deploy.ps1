param (
    [switch]$Deploy = $true,
    [switch]$StartTomcat = $false
)

$ErrorActionPreference = "Stop"

$ProjectRoot = "c:\Users\Usuario\.antigravity-ide\PHPJava\backend-java-maven"
$TomcatLib = "C:\Servidores\Apache Software Foundation\Tomcat 11.0\lib"
$TomcatWebapps = "C:\Servidores\Apache Software Foundation\Tomcat 11.0\webapps"

$M2Repo = "C:\Users\Usuario\.m2\repository"
$MysqlJar = "$M2Repo\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar"
$GsonJar = "$M2Repo\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"
$ServletJar = "$TomcatLib\servlet-api.jar"

Write-Host "=== Compilando Backend Java Web SVIS ===" -ForegroundColor Cyan

# Validar existencia de dependencias
if (!(Test-Path $ServletJar)) { throw "No se encuentra servlet-api.jar en $ServletJar" }
if (!(Test-Path $MysqlJar)) { throw "No se encuentra mysql-connector-j en $MysqlJar" }
if (!(Test-Path $GsonJar)) { throw "No se encuentra gson en $GsonJar" }

$TargetDir = "$ProjectRoot\target\svis"
$ClassesDir = "$TargetDir\WEB-INF\classes"
$LibDir = "$TargetDir\WEB-INF\lib"

# Limpieza y preparación de directorios
if (Test-Path $TargetDir) { Remove-Item -Path $TargetDir -Recurse -Force }
New-Item -ItemType Directory -Path $ClassesDir -Force | Out-Null
New-Item -ItemType Directory -Path $LibDir -Force | Out-Null

# Copiar bibliotecas necesarias a WEB-INF/lib
Copy-Item $MysqlJar -Destination $LibDir -Force
Copy-Item $GsonJar -Destination $LibDir -Force

# Copiar web.xml
$WebXmlSrc = "$ProjectRoot\src\main\webapp\WEB-INF\web.xml"
Copy-Item $WebXmlSrc -Destination "$TargetDir\WEB-INF\web.xml" -Force

# Listar todos los archivos .java
$JavaFiles = Get-ChildItem -Path "$ProjectRoot\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
Write-Host "Encontrados $($JavaFiles.Count) archivos Java para compilar..." -ForegroundColor Yellow

$Classpath = "$ServletJar;$MysqlJar;$GsonJar"

# Ejecutar compilador javac
& javac -encoding UTF-8 -cp $Classpath -d $ClassesDir $JavaFiles

if ($LASTEXITCODE -ne 0) {
    throw "Fallo en la compilación Java."
}
Write-Host "Compilación exitosa." -ForegroundColor Green

# Generar archivo WAR
$WarPath = "$ProjectRoot\target\svis.war"
if (Test-Path $WarPath) { Remove-Item $WarPath -Force }

$JarExe = "C:\Program Files\Java\jdk-24\bin\jar.exe"
if (Test-Path $JarExe) {
    Set-Location $TargetDir
    & $JarExe -cvf $WarPath * | Out-Null
    Set-Location $ProjectRoot
} else {
    Compress-Archive -Path "$TargetDir\*" -DestinationPath "$ProjectRoot\target\svis.zip" -Force
    Rename-Item "$ProjectRoot\target\svis.zip" "svis.war" -Force
}
Write-Host "Archivo WAR generado en: $WarPath" -ForegroundColor Green

# Desplegar en Tomcat si está habilitado
if ($Deploy) {
    Write-Host "Desplegando en Apache Tomcat ($TomcatWebapps)..." -ForegroundColor Cyan
    $DestAppDir = "$TomcatWebapps\svis"
    if (Test-Path $DestAppDir) {
        Remove-Item $DestAppDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    Copy-Item $WarPath -Destination "$TomcatWebapps\svis.war" -Force
    # También desplegar carpeta descomprimida para arranque instantáneo
    Copy-Item $TargetDir -Destination $DestAppDir -Recurse -Force
    Write-Host "Despliegue completado exitosamente en Tomcat (contexto /svis)." -ForegroundColor Green
}

if ($StartTomcat) {
    Write-Host "Iniciando servicio de Tomcat..." -ForegroundColor Cyan
    Start-Service Tomcat11 -ErrorAction SilentlyContinue
}
