param(
    [string]$Issuer = 'http://localhost:8180/realms/healthlink'
)

$ErrorActionPreference = 'Stop'
$device = Invoke-RestMethod -Method Post -Uri "$Issuer/protocol/openid-connect/auth/device" -Body @{
    client_id = 'healthlink-cli'
    scope = 'openid'
}
Write-Host "Open $($device.verification_uri) and enter $($device.user_code), then sign in with a HealthLink realm user."
$deadline = (Get-Date).AddSeconds($device.expires_in)
$interval = [Math]::Max(1, $device.interval)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds $interval
    try {
        $response = Invoke-RestMethod -Method Post -Uri "$Issuer/protocol/openid-connect/token" -Body @{
            grant_type = 'urn:ietf:params:oauth:grant-type:device_code'
            device_code = $device.device_code
            client_id = 'healthlink-cli'
        }
        return $response.access_token
    } catch {
        $failure = $_.ErrorDetails.Message | ConvertFrom-Json
        if ($failure.error -eq 'slow_down') {
            $interval += 5
        } elseif ($failure.error -ne 'authorization_pending') {
            throw
        }
    }
}
throw 'The device code expired. Run this script again to sign in.'
