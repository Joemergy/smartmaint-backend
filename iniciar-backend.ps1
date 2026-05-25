# =============================================================
# Script de arranque del backend Smartmaint
# Libera el puerto 8080 si está ocupado y lanza spring-boot:run
# =============================================================

$PORT = 8080

Write-Host "Verificando puerto $PORT..." -ForegroundColor Cyan
$conn = Get-NetTCPConnection -LocalPort $PORT -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $pids = $conn | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($id in $pids) {
        try {
            $proc = Get-Process -Id $id -ErrorAction SilentlyContinue
            Write-Host "  Cerrando proceso: $($proc.ProcessName) (PID $id)..." -ForegroundColor Yellow
            Stop-Process -Id $id -Force
        } catch {
            Write-Host "  No se pudo cerrar PID ${id}" -ForegroundColor Red
        }
    }
    Start-Sleep -Seconds 1
    $still = Get-NetTCPConnection -LocalPort $PORT -State Listen -ErrorAction SilentlyContinue
    if ($still) {
        Write-Host "ERROR: No se pudo liberar el puerto $PORT. Abortando." -ForegroundColor Red
        exit 1
    }
}
Write-Host "Puerto $PORT libre. Iniciando backend..." -ForegroundColor Green

# Directorio del script
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Arrancar backend
.\mvnw.cmd spring-boot:run
