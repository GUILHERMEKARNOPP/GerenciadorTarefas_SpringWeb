# Cria o banco gerenciador_web no PostgreSQL deste PC e guarda a senha na variável WEB_DB_PASSWORD.
# Uso (na raiz do repositório):  powershell -ExecutionPolicy Bypass -File banco\setup-banco.ps1
# Pode rodar de novo sem problema: se o banco já existir, só troca a senha.

$ErrorActionPreference = 'Stop'
$psql = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' |
    Sort-Object { [int]$_.Directory.Parent.Name } -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $psql) { throw 'psql.exe nao encontrado em C:\Program Files\PostgreSQL. Instale o PostgreSQL 16 ou mais novo.' }

function Ler-Senha([string]$mensagem) {
    $segura = Read-Host -AsSecureString $mensagem
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

$senhaPostgres = Ler-Senha 'Senha do usuario postgres (definida na instalacao do PostgreSQL)'
$senhaApp = Ler-Senha 'Nova senha para o usuario gerenciador_web (minimo 8 caracteres)'
$confirmacao = Ler-Senha 'Confirme a senha do gerenciador_web'
if ($senhaApp -ne $confirmacao) { throw 'As senhas do gerenciador_web nao conferem.' }
if ($senhaApp.Length -lt 8) { throw 'A senha do gerenciador_web precisa ter pelo menos 8 caracteres.' }

try {
    $env:PGPASSWORD = $senhaPostgres
    $env:APP_DB_PASSWORD = $senhaApp
    & $psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f (Join-Path $PSScriptRoot 'criar-banco.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar o banco. Confira a senha do postgres e rode de novo.' }

    # Variável de usuário do Windows lida pelo application.properties do sistema.
    [Environment]::SetEnvironmentVariable('WEB_DB_PASSWORD', $senhaApp, 'User')
    Write-Host "`nPronto! Banco gerenciador_web criado e WEB_DB_PASSWORD configurada." -ForegroundColor Green
    Write-Host 'As tabelas sao criadas pelo proprio sistema na primeira vez que ele subir (executar.ps1).'
    Write-Host 'Reabra o VS Code/IntelliJ para eles enxergarem a variavel nova.'
}
finally {
    Remove-Item Env:PGPASSWORD, Env:APP_DB_PASSWORD -ErrorAction SilentlyContinue
}
