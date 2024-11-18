# Function to extract token from JSON response
function Extract-Token {
    param (
        [string]$jsonResponse
    )
    $tokenObj = $jsonResponse | ConvertFrom-Json
    return $tokenObj.access_token
}

# OAuth2 token request parameters
$tokenEndpoint = "http://127.0.0.1:54353/oauth2/token"
$clientId = "ThisIsMyClientId"
$clientSecret = "myClientSecret"
$resourceEndpoint = "http://127.0.0.1:54356/sample/user-info"

# Prepare authentication header (Base64 encoded client_id:client_secret)
$auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${clientId}:${clientSecret}"))
$headers = @{
    "Authorization" = "Basic $auth"
    "Content-Type" = "application/x-www-form-urlencoded"
    "cache-type" = "hazelcast"
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

} catch {
    Write-Host "Error occurred: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Status Description: $($_.Exception.Response.StatusDescription)"
}