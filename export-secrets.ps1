$keystorePath = "C:\Users\Jed\Desktop\Gia Family Control\parent-app\parent-app-release.jks"
$googleServicesPath = "C:\Users\Jed\Desktop\Gia Family Control\parent-app\app\google-services.json"
$outputFile = "C:\Users\Jed\Desktop\github-secrets.txt"

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
$googleServicesBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($googleServicesPath))

$content = @"
===== GITHUB SECRETS FOR PARENT APP =====

--- PARENT_KEYSTORE_BASE64 ---
$keystoreBase64

--- PARENT_GOOGLE_SERVICES_JSON ---
$googleServicesBase64

--- PARENT_STORE_PASSWORD ---
redprince

--- PARENT_KEY_PASSWORD ---
redprince

==========================================
"@

$content | Out-File -FilePath $outputFile -Encoding UTF8
Write-Host "Done! Saved to: $outputFile"
Start-Process notepad.exe $outputFile
