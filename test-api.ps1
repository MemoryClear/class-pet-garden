# class-pet-garden API Test Script
$base = "http://localhost:8080"

function test($method, $path, $headers, $body) {
    $url = $base + $path
    try {
        $params = @{Uri=$url; Method=$method; ContentType="application/json"}
        if ($headers) { $params.Headers = $headers }
        if ($body) { $params.Body = $body }
        $r = Invoke-WebRequest @params -UseBasicParsing -ErrorAction SilentlyContinue
        Write-Host "$method $path => HTTP $($r.StatusCode)" -ForegroundColor Green
        return $r
    } catch {
        Write-Host "$method $path => ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== Public Endpoints ===" -ForegroundColor Yellow
test "GET" "/"
test "GET" "/home"
test "GET" "/activate"
test "GET" "/api/settings"
test "GET" "/api/pets"

Write-Host "`n=== Protected Endpoints (no auth, expect 403) ===" -ForegroundColor Yellow
test "GET" "/api/students"
test "GET" "/api/history"

Write-Host "`n=== Get Token for Auth Tests ===" -ForegroundColor Yellow
# Find an existing teacher
$r = Invoke-WebRequest -Uri "$base/api/settings" -UseBasicParsing -ErrorAction SilentlyContinue
Write-Host "GET /api/settings => HTTP $($r.StatusCode)"