param(
    [string]$CarpetaApp = (Join-Path $PSScriptRoot '..\backend'),
    [string]$Destino = 'C:\Respaldos\VamosPet',
    [string]$SegundaCarpeta = '',
    [int]$Dias = 14,
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$Servidor = 'localhost',
    [int]$Puerto = 5432,
    [string]$BaseDatos = 'vamospet_db',
    [string]$Usuario = 'app_lectura'
)
$ErrorActionPreference = 'Stop'
if ($Dias -lt 1) { throw 'El parámetro -Dias debe ser 1 o más.' }
$prefijo = 'vamospet_'
$marca = Get-Date -Format 'yyyy-MM-dd_HHmmss'
$carpeta = Join-Path $Destino ($prefijo + $marca)
$registro = Join-Path $Destino 'respaldo.log'

function Anotar {
    param([string]$Texto)
    $linea = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + ' ' + $Texto
    Write-Host $linea
    Add-Content -Path $registro -Value $linea -Encoding UTF8
}

function Comprimir {
    param([string]$Origen, [string]$Archivo)
    if (-not (Test-Path $Origen)) {
        Anotar "AVISO: no existe $Origen; se omite."
        return
    }
    if (-not (Get-ChildItem -Path $Origen -Recurse -File -ErrorAction SilentlyContinue | Select-Object -First 1)) {
        Anotar "AVISO: $Origen está vacía; se omite."
        return
    }
    Compress-Archive -Path (Join-Path $Origen '*') -DestinationPath $Archivo -CompressionLevel Optimal -Force
    Anotar ("Copiado " + $Origen + ' -> ' + (Split-Path $Archivo -Leaf) + ' (' + [math]::Round((Get-Item $Archivo).Length / 1KB) + ' KB)')
}

function Rotar {
    param([string]$Raiz)
    $limite = (Get-Date).AddDays(-$Dias)
    Get-ChildItem -Path $Raiz -Directory -Filter ($prefijo + '*') |
        Where-Object {
            $_.Name -match '^vamospet_(\d{4}-\d{2}-\d{2}_\d{6})(_fallido)?$' -and $_.Name -ne (Split-Path $carpeta -Leaf) -and
            [datetime]::ParseExact($Matches[1], 'yyyy-MM-dd_HHmmss', $null) -lt $limite
        } |
        ForEach-Object {
            Remove-Item -Path $_.FullName -Recurse -Force
            Anotar ("Rotación: se eliminó " + $_.FullName)
        }
}

New-Item -ItemType Directory -Force -Path $Destino | Out-Null
try {
    Anotar "Inicio del respaldo de $BaseDatos en $carpeta"
    $pgDump = Join-Path $PgBin 'pg_dump.exe'
    $pgRestore = Join-Path $PgBin 'pg_restore.exe'
    if (-not (Test-Path $pgDump)) { throw "No se encontró $pgDump. Revisa el parámetro -PgBin." }
    $pgpass = $env:PGPASSFILE
    if (-not $pgpass) { $pgpass = Join-Path $env:APPDATA 'postgresql\pgpass.conf' }
    if (-not (Test-Path $pgpass)) { throw "No se encontró $pgpass. Créalo como indica docs\RESPALDOS.md." }

    New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
    $volcado = Join-Path $carpeta ($BaseDatos + '.backup')
    $errores = Join-Path $carpeta 'pg_dump_errores.txt'
    $ErrorActionPreference = 'Continue'
    & $pgDump -h $Servidor -p $Puerto -U $Usuario -d $BaseDatos -w -F c -Z 6 -f $volcado 2> $errores
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($codigo -ne 0) {
        throw ("pg_dump falló (código $codigo): " + ((Get-Content $errores -Raw) -replace '\s+', ' '))
    }
    if (-not (Test-Path $volcado) -or (Get-Item $volcado).Length -eq 0) { throw 'El archivo del respaldo quedó vacío.' }
    Remove-Item $errores -ErrorAction SilentlyContinue
    $ErrorActionPreference = 'Continue'
    $objetos = (& $pgRestore -l $volcado 2>$null | Measure-Object -Line).Lines
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($codigo -ne 0 -or $objetos -lt 10) { throw 'pg_restore no pudo leer el respaldo recién creado.' }
    Anotar ("Base respaldada: " + [math]::Round((Get-Item $volcado).Length / 1KB) + " KB, $objetos objetos")

    $app = (Resolve-Path $CarpetaApp).Path
    Comprimir (Join-Path $app 'uploads') (Join-Path $carpeta 'uploads.zip')
    Comprimir (Join-Path $app 'uploads_clinicos') (Join-Path $carpeta 'uploads_clinicos.zip')

    $manifiesto = Join-Path $carpeta 'manifiesto.txt'
    $lineas = @("Respaldo Vamos Pet $marca", "Base: $BaseDatos en ${Servidor}:$Puerto (usuario $Usuario)", "Carpeta de la aplicación: $app", '')
    Get-ChildItem -Path $carpeta -File | Where-Object { $_.Name -ne 'manifiesto.txt' } | ForEach-Object {
        $lineas += ('{0}  {1} bytes  SHA256 {2}' -f $_.Name, $_.Length, (Get-FileHash -Path $_.FullName -Algorithm SHA256).Hash)
    }
    Set-Content -Path $manifiesto -Value $lineas -Encoding UTF8

    if ($SegundaCarpeta) {
        New-Item -ItemType Directory -Force -Path $SegundaCarpeta | Out-Null
        Copy-Item -Path $carpeta -Destination $SegundaCarpeta -Recurse -Force
        Anotar "Copia adicional en $SegundaCarpeta"
        Rotar $SegundaCarpeta
    }
    Rotar $Destino
    Anotar 'Respaldo terminado sin errores.'
    exit 0
}
catch {
    Anotar ("ERROR: " + $_.Exception.Message)
    if (Test-Path $carpeta) {
        Rename-Item -Path $carpeta -NewName ((Split-Path $carpeta -Leaf) + '_fallido')
        Anotar ("La carpeta incompleta quedó como " + (Split-Path $carpeta -Leaf) + '_fallido')
    }
    exit 1
}
