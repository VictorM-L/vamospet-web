$ErrorActionPreference = 'Stop'
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'Instala GitHub CLI (https://cli.github.com) y ejecuta gh auth login.' }
$repo = 'VictorM-L/vamospet-web'
$hitos = @(
    @{ t = 'Etapa 3 - Cirugías, grooming y viajes (MK22)'; e = 'closed' },
    @{ t = 'Etapa 4 - Banners de la tienda (MK23)'; e = 'closed' },
    @{ t = 'Etapa 5 - Contacto y configuración (MK24)'; e = 'closed' },
    @{ t = 'Etapa 6 - Venta con receta (MK25 y MK26)'; e = 'closed' },
    @{ t = 'Etapa 7 - Paleta verde (MK27)'; e = 'closed' },
    @{ t = 'Etapa 8 - Seguridad (MK28 a MK30)'; e = 'closed' },
    @{ t = 'Etapa 9 - Git, pruebas y operación (MK31)'; e = 'open' },
    @{ t = 'Etapa 9 - Respaldos y documentación (MK32)'; e = 'open' }
)
$existentes = (& gh api "repos/$repo/milestones?state=all&per_page=100" | ConvertFrom-Json).title
foreach ($h in $hitos) {
    if ($existentes -contains $h.t) { Write-Host "Ya existe: $($h.t)"; continue }
    & gh api "repos/$repo/milestones" -f title="$($h.t)" -f state="$($h.e)" | Out-Null
    Write-Host "Creado: $($h.t)" -ForegroundColor Green
}
$etiquetas = @(
    @{ n = 'etapa-9'; c = '158A55' }, @{ n = 'pruebas'; c = '0E7490' }, @{ n = 'seguridad'; c = 'B91C1C' },
    @{ n = 'documentacion'; c = '6B7280' }, @{ n = 'error'; c = 'DC2626' }
)
foreach ($e in $etiquetas) {
    & gh label create $e.n --repo $repo --color $e.c --force | Out-Null
    Write-Host "Etiqueta: $($e.n)"
}
