let charts = { hazelcast: null, redis: null };

function showSpinner(cacheType) {
    const spinner = document.getElementById(`${cacheType}Spinner`);
    if (spinner) spinner.style.display = 'inline-block';
}

function hideSpinner(cacheType) {
    const spinner = document.getElementById(`${cacheType}Spinner`);
    if (spinner) spinner.style.display = 'none';
}

async function runBenchmark(cacheType) {
    const button = document.getElementById(`${cacheType}Button`);
    const requestsInput = document.getElementById(`${cacheType}Requests`);
    const resultsDiv = document.getElementById(`${cacheType}Results`);

    button.disabled = true;
    showSpinner(cacheType);
    resultsDiv.innerHTML = '<p>Benchmark in progress...</p>';

    try {
        const response = await fetch('/api/benchmark/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                numberOfRequests: parseInt(requestsInput.value),
                cacheType: cacheType
            })
        });
        const data = await response.json();
        displayResults(data, cacheType);
    } catch (error) {
        resultsDiv.innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
    } finally {
        button.disabled = false;
        hideSpinner(cacheType);
    }
}

async function runCombinedBenchmark() {
    const combinedButton = document.getElementById('combinedButton');
    combinedButton.disabled = true;
    showSpinner('combined');

    try {
        // Run Redis benchmark first
        await runBenchmark('redis');
        // Then run Hazelcast benchmark
        await runBenchmark('hazelcast');
    } finally {
        combinedButton.disabled = false;
        hideSpinner('combined');
    }
}

function displayResults(data, cacheType) {
    const resultsDiv = document.getElementById(`${cacheType}Results`);
    const metricsHtml = `
                <h3>Summary</h3>
                <div class="metrics">
                    <div class="metric-card">
                        <h4>OAuth Requests</h4>
                        <p>Average: ${data.avgOAuthTime.toFixed(2)}ms</p>
                        <p>Min: ${data.minOAuthTime.toFixed(2)}ms</p>
                        <p>Max: ${data.maxOAuthTime.toFixed(2)}ms</p>
                        <p>95th Percentile: ${data.p95OAuthTime.toFixed(2)}ms</p>
                    </div>
                    <div class="metric-card">
                        <h4>Resource Requests</h4>
                        <p>Average: ${data.avgResourceTime.toFixed(2)}ms</p>
                        <p>Min: ${data.minResourceTime.toFixed(2)}ms</p>
                        <p>Max: ${data.maxResourceTime.toFixed(2)}ms</p>
                        <p>95th Percentile: ${data.p95ResourceTime.toFixed(2)}ms</p>
                    </div>
                    <div class="metric-card">
                        <h4>Success Rate</h4>
                        <p>Total Requests: ${data.totalRequests}</p>
                        <p>Successful: ${data.successfulRequests}</p>
                        <p>Failed: ${data.failedRequests}</p>
                        <p>Success Rate: ${((data.successfulRequests / data.totalRequests) * 100).toFixed(2)}%</p>
                    </div>
                </div>
            `;
    resultsDiv.innerHTML = metricsHtml;
    updateChart(data, cacheType);
}

function updateChart(data, cacheType) {
    const ctx = document.getElementById(`${cacheType}Chart`).getContext('2d');

    // Destroy existing chart if it exists
    if (charts[cacheType]) {
        charts[cacheType].destroy();
    }

    const successfulResults = data.results.filter(r => r.success);

    charts[cacheType] = new Chart(ctx, {
        type: 'line',
        data: {
            labels: successfulResults.map(r => r.requestNumber),
            datasets: [
                {
                    label: 'OAuth Request Time',
                    data: successfulResults.map(r => r.oauthRequestTime),
                    borderColor: 'rgb(75, 192, 192)',
                    tension: 0.1
                },
                {
                    label: 'Resource Request Time',
                    data: successfulResults.map(r => r.resourceRequestTime),
                    borderColor: 'rgb(255, 99, 132)',
                    tension: 0.1
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Time (ms)'
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Request Number'
                    }
                }
            }
        }
    });
}