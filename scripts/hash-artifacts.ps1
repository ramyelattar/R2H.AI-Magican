param(
    [string]$Root = "."
)

$targets = Get-ChildItem -Path $Root -Recurse -File |
    Where-Object {
        $_.FullName -match "build[\\/]outputs[\\/].*\\.(aab|apk)$"
    }

if (-not $targets) {
    Write-Error "No APK/AAB artifacts were found under build/outputs."
    exit 1
}

$lines = @()
foreach ($file in $targets) {
    $hash = Get-FileHash -Algorithm SHA256 -Path $file.FullName
    $relative = Resolve-Path -Relative $file.FullName
    $lines += "{0}  {1}" -f $hash.Hash.ToLowerInvariant(), $relative
}

$lines | Sort-Object | Set-Content -Path "build-artifact-hashes.txt" -Encoding UTF8
Write-Host "Wrote build-artifact-hashes.txt"
