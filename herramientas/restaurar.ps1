param(
    [Parameter(Mandatory = $true)][string]$Respaldo,
    [string]$BaseNueva = 'vamospet_restaurada',
    [string]$BaseActual = 'vamospet_db',
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$Servidor = 'localhost',
    [int]$Puerto = 5432,
    [string]$Administrador = 'postgres',
    [string]$CarpetaApp = '',
    [switch]$Activar
)
$ErrorActionPreference = 'Stop'
foreach ($n in @($BaseNueva, $BaseActual)) { if ($n -notmatch '^[a-z_][a-z0-9_]*$') { throw "Nombre de base no válido: $n (solo minúsculas, números y _)." } }
$psql = Join-Path $PgBin 'psql.exe'
$pgRestore = Join-Path $PgBin 'pg_restore.exe'
foreach ($p in @($psql, $pgRestore)) { if (-not (Test-Path $p)) { throw "No se encontró $p. Revisa -PgBin." } }

$item = Get-Item $Respaldo
if ($item.PSIsContainer) {
    $carpeta = $item.FullName
    if ($carpeta -like '*_fallido') { throw "$carpeta es un respaldo que falló. Elige otro." }
    $volcado = Get-ChildItem -Path $carpeta -Filter '*.backup' | Select-Object -First 1
    if (-not $volcado) { throw "No hay archivo .backup en $carpeta" }
    $volcado = $volcado.FullName
} else {
    $volcado = $item.FullName
    $carpeta = $item.DirectoryName
}

function Sql {
    param([string]$Base, [string]$Consulta)
    $ErrorActionPreference = 'Continue'
    $salida = & $psql -h $Servidor -p $Puerto -U $Administrador -d $Base -w -v ON_ERROR_STOP=1 -At -F '|' -c $Consulta 2>&1
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($codigo -ne 0) { throw ("Falló la consulta en ${Base}: " + ($salida -join ' ')) }
    return ($salida | Where-Object { $_ -is [string] })
}

$conteo = "SELECT table_name, (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), false, true, '')))[1]::text FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name"

Write-Host "Se restaurará $volcado en la base NUEVA '$BaseNueva'. La base '$BaseActual' no se toca." -ForegroundColor Cyan
$clave = Read-Host "Contraseña de '$Administrador' (solo se usa mientras corre este script)" -AsSecureString
$env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave))
try {
    $existe = Sql 'postgres' "SELECT 1 FROM pg_database WHERE datname = '$BaseNueva'"
    if ($existe) { throw "Ya existe la base '$BaseNueva'. Usa otro -BaseNueva o bórrala tú en pgAdmin si es de una prueba anterior." }
    Sql 'postgres' "CREATE DATABASE $BaseNueva" | Out-Null
    $ErrorActionPreference = 'Continue'
    & $pgRestore -h $Servidor -p $Puerto -U $Administrador -d $BaseNueva -w --no-owner --exit-on-error $volcado
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($codigo -ne 0) {
        Sql 'postgres' "DROP DATABASE $BaseNueva" | Out-Null
        throw "pg_restore falló; se quitó la base '$BaseNueva' que se había creado vacía. Revisa el mensaje de arriba."
    }
    Write-Host "Base restaurada en '$BaseNueva'." -ForegroundColor Green

    $nueva = @{}
    foreach ($l in (Sql $BaseNueva $conteo)) { $c = $l.Split('|'); $nueva[$c[0]] = [int64]$c[1] }
    $actualExiste = Sql 'postgres' "SELECT 1 FROM pg_database WHERE datname = '$BaseActual'"
    $actual = @{}
    if ($actualExiste) { foreach ($l in (Sql $BaseActual $conteo)) { $c = $l.Split('|'); $actual[$c[0]] = [int64]$c[1] } }
    $tabla = foreach ($t in ($nueva.Keys | Sort-Object)) {
        [pscustomobject]@{ Tabla = $t; Restaurada = $nueva[$t]; Actual = $(if ($actual.ContainsKey($t)) { $actual[$t] } else { '-' }) }
    }
    $tabla | Format-Table -AutoSize
    $informe = Join-Path $carpeta ("verificacion_restauracion_" + (Get-Date -Format 'yyyy-MM-dd_HHmm') + '.txt')
    $tabla | Format-Table -AutoSize | Out-String | Set-Content -Path $informe -Encoding UTF8
    Write-Host "Comparación guardada en $informe" -ForegroundColor Green

    $destinoArchivos = if ($CarpetaApp) { (Resolve-Path $CarpetaApp).Path } else { Join-Path $carpeta 'archivos_restaurados' }
    foreach ($z in @('uploads', 'uploads_clinicos')) {
        $zip = Join-Path $carpeta ($z + '.zip')
        if (Test-Path $zip) {
            $hacia = Join-Path $destinoArchivos $z
            New-Item -ItemType Directory -Force -Path $hacia | Out-Null
            Expand-Archive -Path $zip -DestinationPath $hacia -Force
            Write-Host "Archivos de $z restaurados en $hacia"
        }
    }

    if ($Activar) {
        Write-Host ''
        Write-Host "Vas a poner '$BaseNueva' en lugar de '$BaseActual'. La actual NO se borra: queda renombrada." -ForegroundColor Yellow
        Write-Host 'Detén el backend antes de continuar.' -ForegroundColor Yellow
        $confirmacion = Read-Host "Escribe $BaseActual para confirmar"
        if ($confirmacion -ne $BaseActual) { Write-Host 'Cancelado. No se cambió nada.'; return }
        $anterior = $BaseActual + '_anterior_' + (Get-Date -Format 'yyyyMMdd_HHmm')
        if ($actualExiste) { Sql 'postgres' "ALTER DATABASE $BaseActual RENAME TO $anterior" | Out-Null }
        Sql 'postgres' "ALTER DATABASE $BaseNueva RENAME TO $BaseActual" | Out-Null
        Write-Host "Listo: '$BaseActual' es ahora el respaldo. La base anterior quedó como '$anterior'." -ForegroundColor Green
        Write-Host 'Ejecuta db\17_roles_postgres.sql sobre la base restaurada para devolver los permisos de app_escritura y app_lectura.' -ForegroundColor Green
    }
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
