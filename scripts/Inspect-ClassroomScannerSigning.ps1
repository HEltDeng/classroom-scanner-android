[CmdletBinding()]
param(
    [string]$ConfigPath = (Join-Path ([Environment]::GetFolderPath("LocalApplicationData")) "ClassroomScanner\Secrets\android-signing.clixml")
)

$ErrorActionPreference = "Stop"

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

if (-not (Test-Path -LiteralPath $ConfigPath -PathType Leaf)) {
    throw "DPAPI signing configuration was not found: $ConfigPath"
}
$record = Import-Clixml -LiteralPath $ConfigPath
$keytool = "D:\Codex\90_Shared-公共资源\toolchains\jdk-17\jdk-17.0.19+10\bin\keytool.exe"
if (-not (Test-Path -LiteralPath $keytool -PathType Leaf)) {
    $keytoolCommand = Get-Command keytool.exe -ErrorAction Stop
    $keytool = $keytoolCommand.Source
}

$startInfo = [Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $keytool
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
foreach ($argument in @("-J-Duser.language=en", "-J-Duser.country=US", "-list", "-v", "-keystore", $record.StoreFile, "-storetype", "JKS")) {
    $startInfo.ArgumentList.Add($argument)
}
$process = [Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$null = $process.Start()
$process.StandardInput.WriteLine((ConvertTo-PlainText $record.StorePassword))
$process.StandardInput.Close()
$output = $process.StandardOutput.ReadToEnd()
$errorOutput = $process.StandardError.ReadToEnd()
$process.WaitForExit()
if ($process.ExitCode -ne 0) {
    throw "keytool could not inspect the signing identity (exit code $($process.ExitCode))."
}

$lines = $output -split "`r?`n"
$aliasLine = $lines | Where-Object { $_ -match "Alias name:" } | Select-Object -First 1
$validityLine = $lines | Where-Object { $_ -match "Valid from:" } | Select-Object -First 1
$digestLine = $lines | Where-Object { $_ -match "SHA256:" } | Select-Object -First 1
Write-Output "Keystore: $([IO.Path]::GetFullPath($record.StoreFile))"
Write-Output "Alias: $(if ($aliasLine) { $aliasLine.Trim() } else { 'not detected' })"
Write-Output "Validity: $(if ($validityLine) { $validityLine.Trim() } else { 'not detected' })"
Write-Output "Certificate SHA-256: $(if ($digestLine) { $digestLine.Trim() } else { 'not detected' })"
