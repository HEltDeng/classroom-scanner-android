[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    throw "APK was not found: $ApkPath"
}

$apksigner = Join-Path "D:\Codex\90_Shared-公共资源\toolchains\android-sdk\build-tools\36.1.0" "apksigner.bat"
if (-not (Test-Path -LiteralPath $apksigner -PathType Leaf)) {
    throw "Android SDK apksigner.bat was not found."
}

$hash = (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToUpperInvariant()
$verification = & $apksigner verify --verbose --print-certs $ApkPath 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed."
}

Write-Output "APK: $([IO.Path]::GetFullPath($ApkPath))"
Write-Output "SHA-256: $hash"
Write-Output ($verification.Trim())
