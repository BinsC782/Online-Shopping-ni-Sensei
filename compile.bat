@echo off
echo Compiling Online Shopping Application...
cd src
javac -Xlint:none com/shopping/OnlineShoppingApp.java com/shopping/data/FileHandler.java com/shopping/model/Product.java com/shopping/model/User.java com/shopping/ui/CartUi/*.java com/shopping/ui/CheckOutUi/*.java com/shopping/ui/LoginUi/*.java com/shopping/ui/ProductUi/*.java com/shopping/ui/MainProgram/*.java
if %errorlevel% equ 0 (
    echo Compilation successful! No errors or warnings.
    echo.
    echo To run the application, use: java com.shopping.OnlineShoppingApp
) else (
    echo Compilation failed. Please check the errors above.
)
cd ..
pause