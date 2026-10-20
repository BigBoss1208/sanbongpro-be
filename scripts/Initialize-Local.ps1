param([switch]$Isolated)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
function New-LocalSecret {
    $bytes = New-Object byte[] 48
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes); return [Convert]::ToBase64String($bytes) }
    finally { $random.Dispose() }
}
$localFile = Join-Path $projectRoot '.env.local'
if (-not (Test-Path -LiteralPath $localFile)) {
    [IO.File]::WriteAllText($localFile, "JWT_SECRET=$(New-LocalSecret)`n", (New-Object Text.UTF8Encoding($false)))
    Write-Host 'Created ignored .env.local with a cryptographically random JWT secret.'
} else {
    $existing = [IO.File]::ReadAllText($localFile)
    $keyLine = [regex]::Match($existing, '(?m)^\s*JWT_SECRET\s*[:=][^\r\n]*')
    if (-not $keyLine.Success) {
        [IO.File]::AppendAllText($localFile, "`nJWT_SECRET=$(New-LocalSecret)`n", (New-Object Text.UTF8Encoding($false)))
        Write-Host 'Added missing JWT_SECRET to ignored .env.local; other settings preserved.'
    } elseif ($keyLine.Value -match '^\s*JWT_SECRET\s*[:=]\s*$') {
        $updated = $existing.Remove($keyLine.Index, $keyLine.Length).Insert($keyLine.Index, "JWT_SECRET=$(New-LocalSecret)")
        [IO.File]::WriteAllText($localFile, $updated, (New-Object Text.UTF8Encoding($false)))
        Write-Host 'Initialized empty JWT_SECRET; other settings preserved.'
    } else {
        Write-Host 'Preserved existing .env.local. No secret was replaced.'
    }
}
if ($Isolated) {
    $smokeFile = Join-Path $projectRoot '.env.smoke'
    if (-not (Test-Path -LiteralPath $smokeFile)) {
        $settings = @(
            'spring.datasource.url=jdbc:h2:mem:local-smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'spring.datasource.driver-class-name=org.h2.Driver',
            'spring.datasource.username=sa',
            "spring.datasource.password=$(New-LocalSecret)",
            'spring.jpa.hibernate.ddl-auto=create-drop',
            'minio.endpoint=http://127.0.0.1:9000',
            "minio.access-key=$(New-LocalSecret)",
            "minio.secret-key=$(New-LocalSecret)",
            'minio.bucket=local-smoke',
            'vnpay.tmn-code=LOCAL_UNCONFIGURED',
            "vnpay.hash-secret=$(New-LocalSecret)",
            'vnpay.return-url=http://127.0.0.1:8084/api/thanh-toan/vnpay/return',
            'app.cors-origins=http://localhost:3004,http://127.0.0.1:4319',
            'server.port=8084'
        )
        [IO.File]::WriteAllText($smokeFile, ($settings -join "`n") + "`n", (New-Object Text.UTF8Encoding($false)))
    }
    Write-Host 'Isolated smoke configuration ready: H2 memory database, loopback only; external services are unconfigured.'
}
Write-Host 'Run from the repository root. Use profile dev with your real local service environment, or smoke for isolated verification.'
