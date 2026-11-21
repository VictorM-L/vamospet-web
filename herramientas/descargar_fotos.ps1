$ErrorActionPreference = 'Stop'
$destino = Join-Path $PSScriptRoot '..\frontend\img\fotos'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
$fotos = [ordered]@{
    'perro-beagle.jpg' = 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?q=80&w=800&auto=format&fit=crop'
    'cachorro.jpg' = 'https://images.unsplash.com/photo-1591561582301-7ce6588cc286?q=80&w=800&auto=format&fit=crop'
    'laboratorio.jpg' = 'https://images.unsplash.com/photo-1579684385127-1ef15d508118?q=80&w=800&auto=format&fit=crop'
    'perro-veterinario.jpg' = 'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?q=80&w=800&auto=format&fit=crop'
    'registro.jpg' = 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?q=80&w=800&auto=format&fit=crop'
}
foreach ($foto in $fotos.GetEnumerator()) {
    $archivo = Join-Path $destino $foto.Key
    if (Test-Path $archivo) {
        Write-Host "Ya existe: $($foto.Key)"
        continue
    }
    Invoke-WebRequest -Uri $foto.Value -OutFile $archivo -UseBasicParsing
    Write-Host "Descargada: $($foto.Key)"
}
Write-Host "Listo. Fotos en $destino"
