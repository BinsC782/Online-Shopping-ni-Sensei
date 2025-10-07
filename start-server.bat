@echo off
setlocal

echo ===================================
echo  Online Shopping App - Start Server
echo ===================================

set BIN=target\classes
set LIBS=lib\*

if not exist "%BIN%\com\shopping\ServerMain.class" (
    echo Error: Server not compiled. Please run 'run-server.bat' first.
    pause
    exit /b 1
)

echo Starting server...
echo ===================================
echo  Server running at http://localhost:8080
echo  Press Ctrl+C to stop the server
echo ===================================

java -cp "%BIN%;%LIBS%" com.shopping.ServerMain

if errorlevel 1 (
    echo.
    echo Server failed to start. Check if port 8080 is available.
    pause
)

endlocal
