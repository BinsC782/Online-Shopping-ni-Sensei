@echo off
setlocal

echo ===================================
echo  Online Shopping App - Build & Run
echo ===================================

:: Set up environment
set SRC=src\main\java
set BIN=target\classes
set LIBS=lib\*

:: Create target directory if it doesn't exist
if not exist "%BIN%" mkdir "%BIN%"

echo Compiling Java source files...

:: Compile all Java files
javac -d "%BIN%" -cp "%LIBS%" "%SRC%\com\shopping\*.java"

if errorlevel 1 (
    echo.
    echo Compilation failed. Please check the errors above.
    pause
    exit /b 1
)

echo.
echo Compilation successful! Starting server...

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
