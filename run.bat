@echo off
echo ===================================
echo  Online Shopping App - JavaFX Launch
echo ===================================

:: Run the application using Maven JavaFX plugin
mvn javafx:run

if errorlevel 1 (
    echo.
    echo Application launch failed.
    echo Make sure the project compiles successfully.
    pause
    exit /b 1
)

echo.
echo Application launched successfully!