# Get Minikube IP
$minikubeIp = "192.168.49.2"

# Path to hosts file in Windows
$hostsFile = "$env:windir\System32\drivers\etc\hosts"

# Check if the entry already exists
$existingEntry = Get-Content $hostsFile | Select-String "oauth-client.local"
if (!$existingEntry) {
    # Add entry to hosts file - requires admin privileges
    $hostEntry = "`n$minikubeIp`toauth-client.local"

    # First try to add without elevation
    try {
        Add-Content -Path $hostsFile -Value $hostEntry -ErrorAction Stop
        Write-Host "Successfully added entry to hosts file"
    }
    # If it fails due to permissions, try with elevation
    catch {
        $addHostsEntry = @"
        `$hostsFile = "$env:windir\System32\drivers\etc\hosts"
        Add-Content -Path `$hostsFile -Value "$hostEntry"
"@

        # Create a temporary script
        $tempScript = New-TemporaryFile
        Set-Content -Path $tempScript -Value $addHostsEntry

        # Run the script with elevation
        Start-Process powershell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File $tempScript" -Verb RunAs -Wait

        # Clean up
        Remove-Item $tempScript

        Write-Host "Entry added to hosts file with elevation"
    }
}
else {
    Write-Host "Entry already exists in hosts file"
}

# Verify the entry
Write-Host "`nCurrent hosts file entries for oauth-client.local:"
Get-Content $hostsFile | Select-String "oauth-client.local"

# Test the connection
Write-Host "`nTesting connection to oauth-client.local..."
Test-NetConnection -ComputerName "oauth-client.local" -Port 80

Write-Host "`nYou can now access the application at: http://oauth-client.local/benchmark.html"