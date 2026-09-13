[CmdletBinding()]
param(
    [string]$StoreFile = "D:\Codex\secrets\classroom-scanner-android\android-signing\classroom-scanner-release.jks",
    [string]$KeyAlias = "classroom-scanner-release",
    [string]$SecondaryBackupFile = "F:\ClassroomScannerSecureBackups\classroom-scanner-android\classroom-scanner-release.jks"
)

$ErrorActionPreference = "Stop"

if (Test-Path -LiteralPath $StoreFile) {
    throw "Refusing to overwrite an existing keystore: $StoreFile"
}

$scannerJdk = "D:\Codex\90_Shared-公共资源\toolchains\jdk-17\jdk-17.0.19+10"
$keytool = Join-Path $scannerJdk "bin\keytool.exe"
if (-not (Test-Path -LiteralPath $keytool -PathType Leaf)) {
    $keytoolCommand = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($null -eq $keytoolCommand) {
        throw "JDK 17 keytool.exe was not found."
    }
    $keytool = $keytoolCommand.Source
}

$storeDirectory = Split-Path -Parent $StoreFile
New-Item -ItemType Directory -Path $storeDirectory -Force | Out-Null

$alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
$randomBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
$generatedPassword = -join ($randomBytes | ForEach-Object { $alphabet[$_ % $alphabet.Length] })

$arguments = @(
    "-genkeypair"
    "-alias", $KeyAlias
    "-keyalg", "RSA"
    "-keysize", "4096"
    "-validity", "10000"
    "-dname", "CN=Classroom Scanner Android, OU=Internal, O=Classroom Assessment, L=Singapore, C=SG"
    "-storetype", "JKS"
    "-keystore", $StoreFile
)

$startInfo = [Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $keytool
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
foreach ($argument in $arguments) {
    $startInfo.ArgumentList.Add($argument)
}

$keytoolProcess = [Diagnostics.Process]::new()
$keytoolProcess.StartInfo = $startInfo
$null = $keytoolProcess.Start()
$keytoolProcess.StandardInput.WriteLine($generatedPassword)
$keytoolProcess.StandardInput.WriteLine($generatedPassword)
# The key password uses the store password; RETURN accepts that choice.
$keytoolProcess.StandardInput.WriteLine("")
$keytoolProcess.StandardInput.Close()
$keytoolOutput = $keytoolProcess.StandardOutput.ReadToEnd()
$keytoolError = $keytoolProcess.StandardError.ReadToEnd()
$keytoolProcess.WaitForExit()

if ($keytoolProcess.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $StoreFile -PathType Leaf)) {
    throw "keytool failed while creating the signing identity (exit code $($keytoolProcess.ExitCode))."
}

$scannerLocalAppData = [Environment]::GetFolderPath("LocalApplicationData")
$configDirectory = Join-Path $scannerLocalAppData "ClassroomScanner\Secrets"
$configPath = Join-Path $configDirectory "android-signing.clixml"
New-Item -ItemType Directory -Path $configDirectory -Force | Out-Null
$securePassword = ConvertTo-SecureString $generatedPassword -AsPlainText -Force
[pscustomobject]@{
    StoreFile = [IO.Path]::GetFullPath($StoreFile)
    KeyAlias = $KeyAlias
    StorePassword = $securePassword
    KeyPassword = $securePassword
    RegisteredAt = [DateTime]::UtcNow.ToString("O")
} | Export-Clixml -LiteralPath $configPath -Force

if ($SecondaryBackupFile -and (Test-Path -LiteralPath (Split-Path -Qualifier $SecondaryBackupFile))) {
    $secondaryDirectory = Split-Path -Parent $SecondaryBackupFile
    New-Item -ItemType Directory -Path $secondaryDirectory -Force | Out-Null
    Copy-Item -LiteralPath $StoreFile -Destination $SecondaryBackupFile -Force
    $backupMessage = "Protected secondary copy created at $SecondaryBackupFile."
} else {
    $backupMessage = "No second fixed volume was available; create a disconnected offline copy before public distribution."
}

Write-Output "New Classroom Scanner signing identity created."
Write-Output "Keystore path: $StoreFile"
Write-Output "Alias: $KeyAlias"
Write-Output "DPAPI config path: $configPath"
Write-Output $backupMessage
