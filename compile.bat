@echo off
setlocal

echo ===================================
echo  Online Shopping App - JavaFX Build
echo ===================================

:: Set up JavaFX paths - Update this path to match your JavaFX installation
set JAVAFX_PATH=C:\Users\Carlos\Desktop\javafx\lib

:: Set up project paths
set SRC=src\main\java
set BIN=target\classes
set RESOURCES=src\main\resources

:: Create target directory if it doesn't exist
if not exist "%BIN%" mkdir "%BIN%"
if not exist "%RESOURCES%" mkdir "%RESOURCES%"

echo Compiling Java source files with JavaFX...

:: Compile all Java files with JavaFX module path
javac --module-path "lib" ^
      --add-modules javafx.controls,javafx.fxml ^
      -d "%BIN%" ^
      -cp "lib/*" ^
      "%SRC%\com\shopping\ShoppingApplication.java" ^
      "%SRC%\com\shopping\MainController.java" ^
      "%SRC%\com\shopping\service\ShoppingService.java" ^
      "%SRC%\com\shopping\data\FileHandler.java" ^
      "%SRC%\com\shopping\model\Product.java" ^
      "%SRC%\com\shopping\model\User.java" ^
      "%SRC%\com\shopping\model\Order.java" ^
      "%SRC%\com\shopping\model\OrderItem.java" ^
      "%SRC%\com\shopping\model\Cart.java" ^
      "%SRC%\com\shopping\util\ApiResponse.java" ^
      "%SRC%\com\shopping\util\JsonUtils.java" ^
      "%SRC%\com\shopping\util\ValidationUtil.java"

if errorlevel 1 (
    echo.
    echo Compilation failed. Please check the errors above.
    echo Make sure JavaFX SDK is installed correctly.
    pause
    exit /b 1
)

echo.
echo Compilation successful!

endlocal
