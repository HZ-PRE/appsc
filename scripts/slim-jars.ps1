param(
    [Parameter(Mandatory = $true)]
    [string]$LibDir
)

Add-Type -AssemblyName System.IO.Compression.FileSystem

$excludePrefixes = @(
    'META-INF/maven/',
    'META-INF/versions/9/module-info.class'
)

$excludeExact = @(
    'META-INF/INDEX.LIST',
    'module-info.class'
)

function Should-ExcludeEntry([string]$name) {
    $normalized = $name -replace '\\', '/'
    if ($normalized.EndsWith('/')) { return $false }

    foreach ($prefix in $excludePrefixes) {
        if ($normalized.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    foreach ($exact in $excludeExact) {
        if ($normalized.Equals($exact, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    if ($normalized -match '^META-INF/.*\.(SF|RSA|DSA|EC)$') { return $true }
    if ($normalized -match '(^|/)(LICENSE|LICENSE\.txt|NOTICE|NOTICE\.txt|README|README\.md|CHANGELOG|about\.html)$') { return $true }
    if ($normalized -match '(^|/)(package-list|element-list)$') { return $true }
    if ($normalized -match '\.(asc|md5|sha1|sha256)$') { return $true }

    return $false
}

Get-ChildItem -Path $LibDir -Filter '*.jar' -File | ForEach-Object {
    $jarPath = $_.FullName
    $tmpPath = "$jarPath.tmp"
    if (Test-Path $tmpPath) { Remove-Item $tmpPath -Force }

    $inputZip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $outputZip = [System.IO.Compression.ZipFile]::Open($tmpPath, [System.IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach ($entry in $inputZip.Entries) {
                $entryName = $entry.FullName
                if (Should-ExcludeEntry $entryName) { continue }

                $newEntry = $outputZip.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
                if ($entryName.EndsWith('/')) { continue }

                $inStream = $entry.Open()
                try {
                    $outStream = $newEntry.Open()
                    try {
                        $inStream.CopyTo($outStream)
                    } finally {
                        $outStream.Dispose()
                    }
                } finally {
                    $inStream.Dispose()
                }
            }
        } finally {
            $outputZip.Dispose()
        }
    } finally {
        $inputZip.Dispose()
    }

    $oldSize = (Get-Item $jarPath).Length
    $newSize = (Get-Item $tmpPath).Length
    if ($newSize -gt 0 -and $newSize -lt $oldSize) {
        Move-Item $tmpPath $jarPath -Force
        Write-Host ("Slimmed {0}: {1:N0} -> {2:N0}" -f $_.Name, $oldSize, $newSize)
    } else {
        Remove-Item $tmpPath -Force -ErrorAction SilentlyContinue
    }
}
