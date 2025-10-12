@echo off
title Online Shopping Cart - Server
set "SERVER_PORT=8080"
set "SERVER_URL=http://localhost:%SERVER_PORT%"

:: Change to the script's directory
cd /d "%~dp0"

:: Build the application with Maven first
echo Building application...
call mvn clean package -q

if errorlevel 1 (
    echo Build failed! Please check for compilation errors.
    pause
    exit /b 1
)

:: Start the server using the JAR file
echo Starting server...
start "Shopping Cart Server" java -jar "target\online-shopping-app-1.0.0.jar"

:: Wait for the server to start
echo Waiting for server to initialize...
timeout /t 5 >nul

:: Open the browser
echo Opening browser...
start "" "%SERVER_URL%"

echo.
echo ==================================================
echo Server started successfully!
echo Access your application at: %SERVER_URL%
echo ==================================================
echo.
echo Press Ctrl+C in the server window to stop the server
echo Or close this window to stop everything
echo.

:: Keep this window open
pause >nul