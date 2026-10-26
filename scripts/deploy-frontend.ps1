#Requires -Version 5.1
<#
.SYNOPSIS
    Compila el SPA de Angular y lo publica en los recursos estáticos del backend.

.DESCRIPTION
    Automatiza el único camino válido para actualizar el frontend embebido:

      1. `npx ng build --configuration production` en el repo del frontend.
      2. Copia el contenido de la salida a `src/main/resources/static/`.
      3. Borra los archivos del bundle anterior que ya no existen en la salida.
      4. Verifica que cada archivo copiado tenga el mismo SHA256 en origen y destino.
      5. Valida que cada hash declarado en `ngsw.json` exista en disco y coincida.

    Falla con un mensaje explícito si cualquier paso no se cumple. Nunca borra
    nada fuera del directorio `static`.

.PARAMETER FrontendRoot
    Raíz del repositorio del frontend Angular.

.PARAMETER OutputPath
    Directorio de salida del build, relativo a `FrontendRoot`.

.PARAMETER BackendStatic
    Directorio destino del backend. Debe terminar en `static`.

.PARAMETER Configuration
    Configuración de Angular que se compila (por defecto `production`).

.EXAMPLE
    .\scripts\deploy-frontend.ps1

.EXAMPLE
    .\scripts\deploy-frontend.ps1 -FrontendRoot D:\repos\frontendmagico
#>
[CmdletBinding()]
param(
    [string]$FrontendRoot = 'C:\dev\frontendmagico',
    [string]$OutputPath = 'dist\icc-tda-frontend\browser',
    [string]$BackendStatic = '',
    [string]$Configuration = 'production'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# `$PSScriptRoot` todavía no existe al evaluar los valores por defecto del param.
if ([string]::IsNullOrWhiteSpace($BackendStatic)) {
    $BackendStatic = Join-Path (Split-Path -Parent $PSScriptRoot) 'src\main\resources\static'
}

function Fail {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ''
    Write-Host "ERROR: $Message" -ForegroundColor Red
    exit 1
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

# ngsw.json declara hashes SHA-1 (40 caracteres), no SHA256.
function Get-Sha1 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
}

function Get-RelativePath {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Path
    )
    return $Path.Substring($Root.Length).TrimStart('\', '/')
}

# --- Rutas -------------------------------------------------------------------

if (-not (Test-Path -LiteralPath $FrontendRoot -PathType Container)) {
    Fail "No existe el repositorio del frontend: $FrontendRoot"
}
$FrontendRoot = (Resolve-Path -LiteralPath $FrontendRoot).Path
$distPath = [System.IO.Path]::GetFullPath((Join-Path $FrontendRoot $OutputPath))

$backendStaticPath = [System.IO.Path]::GetFullPath($BackendStatic)
if (-not (Test-Path -LiteralPath $backendStaticPath -PathType Container)) {
    Fail "No existe el directorio estático del backend: $backendStaticPath"
}
if ((Split-Path -Leaf $backendStaticPath) -ne 'static') {
    Fail "El destino debe ser un directorio 'static' para no borrar nada fuera del bundle: $backendStaticPath"
}
if (-not (Test-Path -LiteralPath (Join-Path $backendStaticPath 'index.html') -PathType Leaf)) {
    Fail "El destino no parece el estático del backend (falta index.html): $backendStaticPath"
}

# --- Build -------------------------------------------------------------------

Write-Host "Compilando Angular ($Configuration) en $FrontendRoot ..." -ForegroundColor Cyan
Push-Location -LiteralPath $FrontendRoot
# Angular escribe los warnings (presupuesto, CommonJS) por stderr. Con
# `$ErrorActionPreference = 'Stop'` PowerShell 5.1 los trata como errores
# terminantes y aborta un build que en realidad fue correcto, así que aquí se
# rebaja la preferencia y el código de salida se evalúa explícitamente.
$buildExitCode = 0
$previousErrorActionPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    # Se invoca a través de cmd para que stdout y stderr lleguen como texto plano
    # en orden: si PowerShell captura stderr de un ejecutable nativo lo convierte
    # en ErrorRecord y, con 'Stop', aborta un build que fue correcto.
    & cmd /c "npx ng build --configuration $Configuration 2>&1"
    $buildExitCode = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
    Pop-Location
}
if ($buildExitCode -ne 0) {
    Fail "ng build terminó con código $buildExitCode."
}

if (-not (Test-Path -LiteralPath $distPath -PathType Container)) {
    Fail "El build no generó el directorio esperado: $distPath"
}
if (-not (Test-Path -LiteralPath (Join-Path $distPath 'index.html') -PathType Leaf)) {
    Fail "La salida del build no contiene index.html: $distPath"
}

$mainBundles = @(Get-ChildItem -LiteralPath $distPath -Filter 'main-*.js' -File)
if ($mainBundles.Count -ne 1) {
    Fail "Se esperaba exactamente un main-*.js en $distPath y hay $($mainBundles.Count)."
}
$mainBundle = $mainBundles[0].Name

$distIndex = Get-Content -LiteralPath (Join-Path $distPath 'index.html') -Raw
if ($distIndex -notmatch [regex]::Escape($mainBundle)) {
    Fail "El index.html del build no referencia el bundle $mainBundle."
}

Write-Host "Bundle compilado: $mainBundle" -ForegroundColor Green

# --- Copia -------------------------------------------------------------------

$sourceFiles = @(Get-ChildItem -LiteralPath $distPath -Recurse -File)
if ($sourceFiles.Count -eq 0) {
    Fail "La salida del build está vacía: $distPath"
}

$sourceRelative = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
foreach ($file in $sourceFiles) {
    [void]$sourceRelative.Add((Get-RelativePath -Root $distPath -Path $file.FullName))
}

# Archivos del bundle anterior que ya no existen: solo se borran bajo static/.
$staleFiles = @(Get-ChildItem -LiteralPath $backendStaticPath -Recurse -File | Where-Object {
        -not $sourceRelative.Contains((Get-RelativePath -Root $backendStaticPath -Path $_.FullName))
    })

foreach ($stale in $staleFiles) {
    if (-not $stale.FullName.StartsWith($backendStaticPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        Fail "Se intentó borrar fuera del estático: $($stale.FullName)"
    }
    Remove-Item -LiteralPath $stale.FullName -Force
}
Write-Host "Archivos obsoletos eliminados: $($staleFiles.Count)"

# Directorios que quedaron vacíos tras el borrado.
$emptyDirs = @(Get-ChildItem -LiteralPath $backendStaticPath -Recurse -Directory |
        Sort-Object { $_.FullName.Length } -Descending |
        Where-Object { -not (Get-ChildItem -LiteralPath $_.FullName -Force) })
foreach ($dir in $emptyDirs) {
    if (-not $dir.FullName.StartsWith($backendStaticPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        Fail "Se intentó borrar fuera del estático: $($dir.FullName)"
    }
    Remove-Item -LiteralPath $dir.FullName -Force
}

foreach ($file in $sourceFiles) {
    $relative = Get-RelativePath -Root $distPath -Path $file.FullName
    $destination = Join-Path $backendStaticPath $relative
    $destinationDir = Split-Path -Parent $destination

    if (-not (Test-Path -LiteralPath $destinationDir -PathType Container)) {
        New-Item -ItemType Directory -Path $destinationDir -Force | Out-Null
    }
    Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
}

# --- Verificación SHA256 -----------------------------------------------------

$different = New-Object 'System.Collections.Generic.List[string]'
foreach ($file in $sourceFiles) {
    $relative = Get-RelativePath -Root $distPath -Path $file.FullName
    $destination = Join-Path $backendStaticPath $relative

    if (-not (Test-Path -LiteralPath $destination -PathType Leaf)) {
        $different.Add("$relative (falta en destino)")
        continue
    }
    if ((Get-Sha256 -Path $file.FullName) -ne (Get-Sha256 -Path $destination)) {
        $different.Add($relative)
    }
}
if ($different.Count -gt 0) {
    Fail ("Archivos con SHA256 distinto:`n  " + ($different -join "`n  "))
}

$destinationFiles = @(Get-ChildItem -LiteralPath $backendStaticPath -Recurse -File)
if ($destinationFiles.Count -ne $sourceFiles.Count) {
    Fail "El destino tiene $($destinationFiles.Count) archivos y el build $($sourceFiles.Count): quedaron restos."
}

Write-Host "Archivos idénticos: $($sourceFiles.Count)/$($sourceFiles.Count)" -ForegroundColor Green

# --- Integridad del service worker ------------------------------------------

$ngswPath = Join-Path $backendStaticPath 'ngsw.json'
if (-not (Test-Path -LiteralPath $ngswPath -PathType Leaf)) {
    Fail "Falta ngsw.json en el estático desplegado."
}

try {
    $ngsw = Get-Content -LiteralPath $ngswPath -Raw | ConvertFrom-Json
}
catch {
    Fail "ngsw.json no es JSON válido: $($_.Exception.Message)"
}

$missing = New-Object 'System.Collections.Generic.List[string]'
$mismatched = New-Object 'System.Collections.Generic.List[string]'
$hashEntries = @($ngsw.hashTable.PSObject.Properties)

foreach ($entry in $hashEntries) {
    $assetPath = Join-Path $backendStaticPath ($entry.Name.TrimStart('/').Replace('/', '\'))
    if (-not (Test-Path -LiteralPath $assetPath -PathType Leaf)) {
        $missing.Add($entry.Name)
        continue
    }
    if ((Get-Sha1 -Path $assetPath) -ne ([string]$entry.Value).ToLowerInvariant()) {
        $mismatched.Add($entry.Name)
    }
}

if ($missing.Count -gt 0 -or $mismatched.Count -gt 0) {
    $detail = @()
    if ($missing.Count -gt 0) { $detail += "faltantes: $($missing -join ', ')" }
    if ($mismatched.Count -gt 0) { $detail += "desajustes: $($mismatched -join ', ')" }
    Fail ("ngsw.json no corresponde a los archivos desplegados (" + ($detail -join '; ') + ").")
}

Write-Host "ngsw.json: $($hashEntries.Count) hashes SHA-1 verificados, 0 faltantes, 0 desajustes" -ForegroundColor Green

$staticIndex = Get-Content -LiteralPath (Join-Path $backendStaticPath 'index.html') -Raw
if ($staticIndex -notmatch [regex]::Escape($mainBundle)) {
    Fail "El index.html desplegado no referencia el bundle $mainBundle."
}

Write-Host ''
Write-Host "Despliegue correcto: $mainBundle en $backendStaticPath" -ForegroundColor Green
exit 0
