# Builds the ReturnLoop backend and prints only errors plus the build result.
$ErrorActionPreference = 'Continue'
Set-Location 'C:\Users\SAMSUNG\OneDrive\Desktop\project\backend'
& ..\mvnw.cmd -DskipTests compile 2>&1 | Select-String -Pattern '\[ERROR\]|BUILD|Compiling' | Select-Object -First 80
exit $LASTEXITCODE
