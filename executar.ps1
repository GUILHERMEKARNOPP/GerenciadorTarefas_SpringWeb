# Sobe o sistema em http://localhost:8080. Precisa do JDK 21+ e do banco criado por banco\setup-banco.ps1.
# Uso (na raiz do repositório):  powershell -ExecutionPolicy Bypass -File executar.ps1

$ErrorActionPreference = 'Stop'

# A variável criada pelo setup só aparece em terminais abertos depois dele; se faltar, lê direto do Windows.
if (-not $env:WEB_DB_PASSWORD) {
    $env:WEB_DB_PASSWORD = [Environment]::GetEnvironmentVariable('WEB_DB_PASSWORD', 'User')
}
if (-not $env:WEB_DB_PASSWORD) {
    throw 'Variavel WEB_DB_PASSWORD nao encontrada. Rode antes: powershell -ExecutionPolicy Bypass -File banco\setup-banco.ps1'
}

& (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
