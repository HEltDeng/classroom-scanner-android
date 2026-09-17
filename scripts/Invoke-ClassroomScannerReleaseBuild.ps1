[CmdletBinding()]
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$GradleTask = ":app:assembleRelease"
)

$ErrorActionPreference = "Stop"
Push-Location $ProjectRoot
$runtimeDirectory = $null
$previousSigningConfig = $env:CLASSROOM_SCANNER_SIGNING_CONFIG

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

try {
    $scannerLocalAppData = [Environment]::GetFolderPath("LocalApplicationData")
    $dpapiConfigPath = Join-Path $scannerLocalAppData "ClassroomScanner\Secrets\android-signing.clixml"
    if (-not (Test-Path -LiteralPath $dpapiConfigPath -PathType Leaf)) {
        throw "DPAPI signing configuration was not found: $dpapiConfigPath"
    }

    $signingRecord = Import-Clixml -LiteralPath $dpapiConfigPath
    if (-not (Test-Path -LiteralPath $signingRecord.StoreFile -PathType Leaf)) {
        throw "Configured keystore file was not found."
    }

    $runtimeDirectory = Join-Path $scannerLocalAppData ("ClassroomScanner\Runs\" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
    $propertiesPath = Join-Path $runtimeDirectory "signing.properties"
    $storeFileProperty = ([IO.Path]::GetFullPath($signingRecord.StoreFile)).Replace("\", "/")
    $storePassword = ConvertTo-PlainText $signingRecord.StorePassword
    $keyPassword = ConvertTo-PlainText $signingRecord.KeyPassword
    $propertiesText = @(
        "storeFile=$storeFileProperty"
        "storePassword=$storePassword"
        "keyAlias=$($signingRecord.KeyAlias)"
        "keyPassword=$keyPassword"
    ) -join [Environment]::NewLine
    # Java Properties.load(InputStream) treats a UTF-8 BOM as part of the first key.
    # Write the short-lived file explicitly without a BOM so Gradle reads storeFile correctly.
    $utf8WithoutBom = [Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllText($propertiesPath, $propertiesText, $utf8WithoutBom)
    $env:CLASSROOM_SCANNER_SIGNING_CONFIG = $propertiesPath

    & ".\gradlew.bat" $GradleTask
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release build failed with exit code $LASTEXITCODE."
    }

    $apkSource = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path -LiteralPath $apkSource -PathType Leaf)) {
        throw "Signed release APK was not produced."
    }
    $releaseDirectory = Join-Path $ProjectRoot "release"
    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    $artifactVersionName = "0.3.0"
    $artifactVersionCode = 6
    $artifactStem = "classroom-scanner-android-v$artifactVersionName-code$artifactVersionCode-release"
    $apkDestination = Join-Path $releaseDirectory ($artifactStem + ".apk")
    Copy-Item -LiteralPath $apkSource -Destination $apkDestination -Force
    $mappingSource = Join-Path $ProjectRoot "app\build\outputs\mapping\release\mapping.txt"
    if (-not (Test-Path -LiteralPath $mappingSource -PathType Leaf)) {
        throw "Release mapping.txt was not produced."
    }
    Copy-Item -LiteralPath $mappingSource -Destination (Join-Path $releaseDirectory ($artifactStem + "-mapping.txt")) -Force
    $apkHash = (Get-FileHash -LiteralPath $apkDestination -Algorithm SHA256).Hash.ToUpperInvariant()
    [IO.File]::WriteAllText((Join-Path $releaseDirectory ($artifactStem + ".sha256")), "$apkHash  $([IO.Path]::GetFileName($apkDestination))$([Environment]::NewLine)")
    [IO.File]::WriteAllText((Join-Path $releaseDirectory ($artifactStem + "-release-notes.txt")), @"
Classroom Scanner Android internal release
versionName: $artifactVersionName
versionCode: $artifactVersionCode
applicationId: com.classroomassessment.scanner
APK: $([IO.Path]::GetFileName($apkDestination))
APK SHA-256: $apkHash
Signing: project-specific release keystore; certificate digest is verified separately.
Scope: shared teacher login, Backend class/test discovery, CameraX Preview/ImageAnalysis/ImageCapture, bundled ML Kit QR, Room queue, WorkManager upload.
"@.TrimStart())
    [IO.File]::WriteAllText((Join-Path $releaseDirectory ($artifactStem + "-version.json")), @"
{
  "applicationId": "com.classroomassessment.scanner",
  "versionName": "$artifactVersionName",
  "versionCode": $artifactVersionCode,
  "apk": "$([IO.Path]::GetFileName($apkDestination))",
  "sha256": "$apkHash"
}
"@.TrimStart())

    $apksigner = Join-Path "D:\Codex\90_Shared-公共资源\toolchains\android-sdk\build-tools\36.1.0" "apksigner.bat"
    if (-not (Test-Path -LiteralPath $apksigner -PathType Leaf)) {
        throw "Android SDK apksigner.bat was not found."
    }
    $certificateOutput = & $apksigner verify --verbose --print-certs $apkDestination 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "apksigner verification failed."
    }
    [IO.File]::WriteAllText((Join-Path $releaseDirectory ($artifactStem + "-signing-certificate.txt")), $certificateOutput)
    Write-Output "Signed release APK: $apkDestination"
    Write-Output "APK SHA-256: $apkHash"
    Write-Output "Signing certificate report: $(Join-Path $releaseDirectory ($artifactStem + "-signing-certificate.txt"))"
} finally {
    if ($null -eq $previousSigningConfig) {
        Remove-Item Env:CLASSROOM_SCANNER_SIGNING_CONFIG -ErrorAction SilentlyContinue
    } else {
        $env:CLASSROOM_SCANNER_SIGNING_CONFIG = $previousSigningConfig
    }
    if ($runtimeDirectory -and (Test-Path -LiteralPath $runtimeDirectory)) {
        Remove-Item -LiteralPath $runtimeDirectory -Recurse -Force -ErrorAction SilentlyContinue
    }
    Pop-Location
}
