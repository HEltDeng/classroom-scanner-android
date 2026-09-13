[CmdletBinding()]
param(
    [string]$StoreFile = "D:\Codex\secrets\classroom-scanner-android\android-signing\classroom-scanner-release.jks",
    [string]$KeyAlias = "classroom-scanner-release"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $StoreFile -PathType Leaf)) {
    throw "Keystore file was not found: $StoreFile"
}

$scannerLocalAppData = [Environment]::GetFolderPath("LocalApplicationData")
$configDirectory = Join-Path $scannerLocalAppData "ClassroomScanner\Secrets"
$configPath = Join-Path $configDirectory "android-signing.clixml"
New-Item -ItemType Directory -Path $configDirectory -Force | Out-Null

$storePassword = Read-Host "Enter the Classroom Scanner keystore password" -AsSecureString
$keyPassword = Read-Host "Enter the Classroom Scanner key password" -AsSecureString

if ($storePassword.Length -eq 0 -or $keyPassword.Length -eq 0) {
    throw "Passwords may not be empty."
}

$record = [pscustomobject]@{
    StoreFile = [IO.Path]::GetFullPath($StoreFile)
    KeyAlias = $KeyAlias
    StorePassword = $storePassword
    KeyPassword = $keyPassword
    RegisteredAt = [DateTime]::UtcNow.ToString("O")
}

# Export-Clixml protects SecureString values with the current Windows user's DPAPI.
$record | Export-Clixml -LiteralPath $configPath -Force
Write-Output "Signing configuration registered in the current user's DPAPI store."
Write-Output "Config path: $configPath"
