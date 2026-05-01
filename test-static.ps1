$base = "http://localhost:8080"

Write-Host "=== Static Assets Test ===" -ForegroundColor Cyan

function test($desc, $method, $path) {
    $url = $base + $path
    try {
        $params = @{Uri=$url; Method=$method; UseBasicParsing=$true; ErrorAction="SilentlyContinue"}
        $r = Invoke-WebRequest @params
        Write-Host "[PASS] $desc => HTTP $($r.StatusCode) (content-type: $($r.Headers['Content-Type']) length: $($r.RawContentLength))" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] $desc => $($_.Exception.Message)" -ForegroundColor Red
    }
}

test "index.html" "GET" "/index.html"
test "/assets/ (any asset)" "GET" "/assets/index-CHViJZGW.css"
test "/assets/ (fallback)" "GET" "/assets/"
test "GET / (SPA root)" "GET" "/"

Write-Host "`n=== Static Assets from frontend build ===" -ForegroundColor Cyan
Get-ChildItem "D:\workspace\class-pet-garden\frontend\dist\assets" -ErrorAction SilentlyContinue | ForEach-Object {
    test "assets/$($_.Name)" "GET" "/assets/$($_.Name)"
}