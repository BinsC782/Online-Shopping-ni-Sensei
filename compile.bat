@echo off
setlocal

echo ===================================
echo  Online Shopping App - JavaFX Build
echo ===================================

:: Set up project paths
set SRC=src\main\java
set BIN=target\classes
set RESOURCES=src\main\resources

:: Create target directory if it doesn't exist
if not exist "%BIN%" mkdir "%BIN%"
if not exist "%RESOURCES%" mkdir "%RESOURCES%"

echo Compiling Java source files with JavaFX...

:: Compile ALL files together (module-info.java LAST to avoid validation errors)
javac --module-path "lib" ^
      --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
      -d "%BIN%" ^
      "%SRC%\com\shopping\ShoppingApplication.java" ^
      "%SRC%\com\shopping\MainController.java" ^
      "%SRC%\com\shopping\CartController.java" ^
      "%SRC%\com\shopping\service\ShoppingService.java" ^
      "%SRC%\com\shopping\data\FileHandler.java" ^
      "%SRC%\com\shopping\model\Product.java" ^
      "%SRC%\com\shopping\model\User.java" ^
      "%SRC%\com\shopping\model\Order.java" ^
      "%SRC%\com\shopping\model\OrderItem.java" ^
      "%SRC%\com\shopping\model\Cart.java" ^
      "%SRC%\module-info.java"

if errorlevel 1 (
    echo.
    echo Compilation failed. Please check the errors above.
    echo Make sure JavaFX libraries are in the lib/ folder.
    echo Required modules: javafx.controls, javafx.fxml, javafx.graphics
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo Output directory: %BIN%

endlocal