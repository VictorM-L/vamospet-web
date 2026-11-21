param(
    [string]$Rama = 'feature/etapa-9-mk31',
    [string[]]$Revisores = @('angelaarc', 'GodLie67', 'StefanoZanabria', 'yllarymaldonado898-beep', 'VictorM-L')
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

function Git-Paso {
    Write-Host "> git $($args -join ' ')" -ForegroundColor Cyan
    & git @args
    if ($LASTEXITCODE -ne 0) { throw "Fallo: git $($args -join ' ')" }
}

if (-not (Test-Path '.git')) { throw 'Ejecuta este script dentro de la carpeta VamosPet_Web que tiene la carpeta .git.' }
$remoto = (& git remote get-url origin)
if ($remoto -notmatch 'VictorM-L/vamospet-web') { throw "El remoto origin no es VictorM-L/vamospet-web: $remoto" }

& git check-ignore -q backend/.env.properties
if ($LASTEXITCODE -ne 0) { throw 'backend/.env.properties no está en el .gitignore. No se sube nada.' }
if (Select-String -Path 'backend/src/main/resources/application.properties' -Pattern 'password=(?!\$\{)' -Quiet) {
    throw 'application.properties tiene una clave escrita. No se sube nada.'
}

Git-Paso fetch origin
& git ls-remote --exit-code --heads origin develop | Out-Null
if ($LASTEXITCODE -ne 0) {
    Git-Paso branch develop origin/main
    Git-Paso push -u origin develop
} else {
    & git show-ref --verify --quiet refs/heads/develop
    if ($LASTEXITCODE -ne 0) { Git-Paso branch develop origin/develop }
}

Git-Paso switch -c $Rama develop
Git-Paso add -A
$cambios = (& git diff --cached --name-only)
if ($cambios | Where-Object { $_ -match '(^|/)\.env\.properties$|\.backup$|(^|/)logs/|(^|/)uploads/' }) {
    Git-Paso reset
    throw 'Entre los cambios hay archivos que no deben subirse (.env.properties, respaldos, logs o uploads).'
}
Write-Host "Archivos en el commit: $($cambios.Count)" -ForegroundColor Green
Git-Paso commit -m 'feat: entrega MK31 (MK22 a MK31)' -m 'Etapas 3 a 8 cerradas y primera parte de la Etapa 9. Detalle en CHANGELOG.md.'
Git-Paso push -u origin $Rama

$url = "https://github.com/VictorM-L/vamospet-web/compare/develop...$($Rama)?expand=1"
if (Get-Command gh -ErrorAction SilentlyContinue) {
    $yo = (& gh api user --jq .login)
    $lista = ($Revisores | Where-Object { $_ -ne $yo }) -join ','
    & gh pr create --base develop --head $Rama --title 'MK31: Etapa 9 parte 1' --body-file '.github/pull_request_template.md' --reviewer $lista
    if ($LASTEXITCODE -ne 0) { Write-Host "No se pudo crear el PR con gh. Ábrelo aquí: $url" -ForegroundColor Yellow }
} else {
    Write-Host "Abre el Pull Request hacia develop aquí: $url" -ForegroundColor Yellow
}
