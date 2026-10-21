$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-home'
$env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.android'
$env:TEMP = Join-Path $PSScriptRoot 'work'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP, outputs | Out-Null
& "$PSScriptRoot\gradlew.bat" --no-daemon --console=plain :app:assembleDebug :app:lintDebug
if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
Copy-Item 'app\build\outputs\apk\debug\app-debug.apk' 'outputs\PicoBatterySound-debug.apk' -Force
Write-Host 'APK: outputs\PicoBatterySound-debug.apk'
