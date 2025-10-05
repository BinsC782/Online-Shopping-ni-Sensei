@echo off
title Online Shopping Cart - Server
set "SERVER_PORT=8080"
set "SERVER_URL=http://localhost:%SERVER_PORT%"

:: Change to the script's directory
cd /d "%~dp0"

:: Start the server in a new window
start "Shopping Cart Server" cmd /c "cd /d bin && java -cp . com.shopping.ServerMain %SERVER_PORT%"

:: Wait a moment for the server to start
timeout /t 3 >nul

:: Open the browser
start "" "%SERVER_URL%"

echo Server started! Access your application at: %SERVER_URL%
echo Press any key to stop the server...
pause >nul

:: Stop the server
taskkill /f /im java.exe >nul 2>&1
echo Server stopped.