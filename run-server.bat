@echo off
echo Starting Localhost HTTP Server (offline)...
cd src
rem Compile server entrypoint plus dependencies (models, data)
javac -Xlint:none com/shopping/ServerMain.java com/shopping/data/FileHandler.java com/shopping/model/Product.java com/shopping/model/User.java
if %errorlevel% neq 0 (
  echo Compile failed. Fix errors above.
  cd ..
  pause
  exit /b 1
)
rem Run bound to 127.0.0.1:8080; static files served from ../web
if exist server_port.txt del server_port.txt >nul 2>&1
start "Server" cmd /c java com.shopping.ServerMain

rem Wait briefly for server_port.txt to be written
set /a _tries=0
:wait_port
set /a _tries+=1
if %_tries% GEQ 40 goto no_port
if not exist server_port.txt (
  powershell -NoProfile -Command "Start-Sleep -Milliseconds 250" >nul 2>&1
  goto wait_port
)
set /p PORT=<server_port.txt
echo Server selected port: %PORT%
start "Browser" "http://127.0.0.1:%PORT%/"
goto done

:no_port
echo Could not detect server port; open http://127.0.0.1:8080/ manually if available.

:done
cd ..
pause
