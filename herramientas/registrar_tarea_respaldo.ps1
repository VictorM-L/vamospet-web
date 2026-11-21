param(
    [string]$CarpetaApp = (Join-Path $PSScriptRoot '..\backend'),
    [string]$Destino = 'C:\Respaldos\VamosPet',
    [string]$SegundaCarpeta = '',
    [string]$Hora = '23:00',
    [switch]$SinSesion,
    [switch]$Quitar
)
$ErrorActionPreference = 'Stop'
$nombre = 'VamosPet - Respaldo diario'

if ($Quitar) {
    Unregister-ScheduledTask -TaskName $nombre -Confirm:$false
    Write-Host "Tarea '$nombre' quitada."
    exit 0
}

$script = (Resolve-Path (Join-Path $PSScriptRoot 'respaldo.ps1')).Path
$app = (Resolve-Path $CarpetaApp).Path
$argumentos = "-NoProfile -ExecutionPolicy Bypass -File `"$script`" -CarpetaApp `"$app`" -Destino `"$Destino`""
if ($SegundaCarpeta) { $argumentos += " -SegundaCarpeta `"$SegundaCarpeta`"" }

$accion = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $argumentos -WorkingDirectory $PSScriptRoot
$disparador = New-ScheduledTaskTrigger -Daily -At $Hora
$ajustes = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Hours 2)
$tipoSesion = if ($SinSesion) { 'S4U' } else { 'Interactive' }
$principal = New-ScheduledTaskPrincipal -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType $tipoSesion -RunLevel Limited

Register-ScheduledTask -TaskName $nombre -Action $accion -Trigger $disparador -Settings $ajustes -Principal $principal `
    -Description 'Respaldo de vamospet_db, uploads y uploads_clinicos con pg_dump y .pgpass' -Force | Out-Null

Write-Host "Tarea '$nombre' registrada: todos los días a las $Hora."
Write-Host "Ejecuta:  powershell.exe $argumentos"
Write-Host "Probar ahora: Start-ScheduledTask -TaskName '$nombre'  y luego revisa $Destino\respaldo.log"
