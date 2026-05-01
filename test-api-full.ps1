$base = "http://localhost:8080"

Write-Host "=== Full API Regression Test ===" -ForegroundColor Cyan

function test($desc, $method, $path, $token, $body) {
    $url = $base + $path
    $params = @{Uri=$url; Method=$method; ContentType="application/json"; UseBasicParsing=$true; ErrorAction="SilentlyContinue"}
    if ($token) { $params.Headers = @{Authorization="Bearer $token"} }
    if ($body) { $params.Body = $body }
    try {
        $r = Invoke-WebRequest @params
        Write-Host "[PASS] $desc => HTTP $($r.StatusCode)" -ForegroundColor Green
        return $r.Content
    } catch {
        Write-Host "[FAIL] $desc => $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n-- Public Endpoints (no auth) --" -ForegroundColor Yellow
test "GET /" "GET" "/" $null $null
test "GET /home" "GET" "/home" $null $null
test "GET /activate" "GET" "/activate" $null $null
test "GET /history" "GET" "/history" $null $null
test "GET /exchange-history" "GET" "/exchange-history" $null $null
test "GET /leaderboard" "GET" "/leaderboard" $null $null
test "GET /shop" "GET" "/shop" $null $null
test "GET /settings" "GET" "/settings" $null $null

Write-Host "`n-- Public API Endpoints --" -ForegroundColor Yellow
test "GET /api/settings (public)" "GET" "/api/settings" $null $null
test "GET /api/pets" "GET" "/api/pets" $null $null

Write-Host "`n-- Protected API Endpoints (no token, expect 403) --" -ForegroundColor Yellow
test "GET /api/students (403)" "GET" "/api/students" $null $null
test "GET /api/history (403)" "GET" "/api/history" $null $null

Write-Host "`n-- Register & Login Flow --" -ForegroundColor Yellow
$regBody = '{"username":"tester_a`'` + '01","password":"Pass1234","confirmPassword":"Pass1234"}'
$reg = Invoke-WebRequest -Uri "$base/api/auth/register" -Method POST -ContentType "application/json" -Body $regBody -UseBasicParsing -ErrorAction SilentlyContinue
if ($reg) {
    Write-Host "[PASS] POST /api/auth/register => HTTP $($reg.StatusCode)" -ForegroundColor Green
    $token = ($reg.Content | ConvertFrom-Json).token
    Write-Host "Token obtained: $($token.Substring(0, [Math]::Min(30, $token.Length)))..." -ForegroundColor Cyan

    Write-Host "`n-- Authenticated API Tests --" -ForegroundColor Yellow
    test "GET /api/students (with token)" "GET" "/api/students" $token $null
    test "GET /api/settings (with token)" "GET" "/api/settings" $token $null
    test "GET /api/history" "GET" "/api/history" $token $null
    test "GET /api/shop/items" "GET" "/api/shop/items" $token $null

    # Activate
    $actBody = '{"code":"memory_clear"}'
    $act = Invoke-WebRequest -Uri "$base/api/auth/activate" -Method POST -ContentType "application/json" -Headers @{Authorization="Bearer $token"} -Body $actBody -UseBasicParsing -ErrorAction SilentlyContinue
    if ($act) {
        Write-Host "[PASS] POST /api/auth/activate => HTTP $($act.StatusCode)" -ForegroundColor Green
        $newToken = ($act.Content | ConvertFrom-Json).token
        Write-Host "`n-- After Activation --" -ForegroundColor Yellow
        test "GET /api/students (activated)" "GET" "/api/students" $newToken $null
        test "GET /api/history" "GET" "/api/history" $newToken $null
        test "GET /api/shop/items" "GET" "/api/shop/items" $newToken $null
        test "GET /api/scores/items" "GET" "/api/scores/items" $newToken $null
        test "GET /api/leaderboard" "GET" "/api/leaderboard" $newToken $null
    } else {
        Write-Host "[FAIL] POST /api/auth/activate" -ForegroundColor Red
    }
} else {
    Write-Host "[INFO] User already registered, trying login..." -ForegroundColor Cyan
    $loginBody = '{"username":"tester_a01","password":"Pass1234"}'
    $login = Invoke-WebRequest -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -UseBasicParsing -ErrorAction SilentlyContinue
    if ($login) {
        Write-Host "[PASS] POST /api/auth/login => HTTP $($login.StatusCode)" -ForegroundColor Green
        $token = ($login.Content | ConvertFrom-Json).token
        test "GET /api/students (with token)" "GET" "/api/students" $token $null
    }
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan