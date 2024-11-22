# Function to extract token from JSON response
function Extract-Token {
    param (
        [string]$jsonResponse
    )
    $tokenObj = $jsonResponse | ConvertFrom-Json
    return $tokenObj.access_token
}

# OAuth2 token request parameters
$tokenEndpoint = "http://127.0.0.1:65273/oauth2/token"
$revokeEndpoint = "http://127.0.0.1:65273/oauth2/revoke"
$clientId = "ThisIsMyClientId"
$clientSecret = "myClientSecret"
$resourceEndpoint = "http://127.0.0.1:65276/sample/user-info"

# Prepare authentication header (Base64 encoded client_id:client_secret)
$auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${clientId}:${clientSecret}"))
$headers = @{
    "Authorization" = "Basic $auth"
    "Content-Type" = "application/x-www-form-urlencoded"
    "cache-type" = "redis"
}

# Prepare request body
$body = "grant_type=authorization_code&client_id=$clientId&client_secret=$clientSecret"

try {
    # Step 1: Get OAuth token
    Write-Host "Requesting OAuth token..."
    $tokenResponse = Invoke-RestMethod `
        -Method Post `
        -Uri $tokenEndpoint `
        -Headers $headers `
        -Body $body `
        -ErrorAction Stop

    $accessToken = $tokenResponse.access_token
    Write-Host "Successfully obtained access token"

    # Step 2: Call resource server with token
    Write-Host "Making request to resource server..."
    $resourceHeaders = @{
        "Authorization" = "Bearer $accessToken"
    }

    $resourceResponse = Invoke-RestMethod `
        -Method Get `
        -Uri $resourceEndpoint `
        -Headers $resourceHeaders `
        -ErrorAction Stop

    Write-Host "Resource server response:"
    $resourceResponse | ConvertTo-Json -Depth 10

    # Step 3: Revoke the token
    Write-Host "Revoking access token..."
    $revokeBody = "token=$accessToken&token_type_hint=access_token"
    $revokeResponse = Invoke-RestMethod `
        -Method Post `
        -Uri $revokeEndpoint `
        -Headers $headers `
        -Body $revokeBody `
        -ErrorAction Stop

    Write-Host "Token successfully revoked"

    Start-Sleep -Seconds 3

    # Step 4: Verify token is revoked by attempting to use it again
    Write-Host "`nAttempting to use revoked token..."
    try {
        $invalidTokenResponse = Invoke-RestMethod `
            -Method Get `
            -Uri $resourceEndpoint `
            -Headers $resourceHeaders `
            -ErrorAction Stop

        Write-Host "Warning: Token is still valid!"
    }
    catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Host "Success: Token properly revoked - received 401 Unauthorized"
            Write-Host "Error details: $($_.Exception.Message)"
        }
        else {
            Write-Host "Unexpected error status: $($_.Exception.Response.StatusCode.value__)"
            Write-Host "Error details: $($_.Exception.Message)"
        }
    }

} catch {
    Write-Host "Error occurred: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Status Description: $($_.Exception.Response.StatusDescription)"
}