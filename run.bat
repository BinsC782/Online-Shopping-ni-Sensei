@echo off
setlocal

echo ===================================
echo  Online Shopping App - JavaFX Run
echo ===================================

:: Set up JavaFX paths - Update this path to match your JavaFX installation
set JAVAFX_PATH=C:\Users\Carlos\Desktop\javafx\lib

:: Set up project paths
set BIN=target\classes
set RESOURCES=src\main\resources

if not exist "%BIN%" (
    echo.
    echo Error: Compiled classes not found. Please run compile.bat first.
    pause
    exit /b 1
)

echo Starting JavaFX Shopping Application...

echo ===================================
echo  Application will open in a new window
echo  Close the window to exit the application
echo ===================================

:: Run the JavaFX application
java --module-path "lib" ^
     --add-modules javafx.controls,javafx.fxml ^
     --enable-native-access=javafx.graphics ^
     -cp "%BIN%;lib/*" ^
     com.shopping.ShoppingApplication

if errorlevel 1 (
    echo.
    echo Application failed to start.
    echo Make sure JavaFX SDK path is correct in this script.
    pause
)

endlocal
