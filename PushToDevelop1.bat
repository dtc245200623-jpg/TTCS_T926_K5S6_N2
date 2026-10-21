@echo off
echo ===================================================
echo PUSH TO GITHUB (Branch: develop1)
echo ===================================================
echo.
echo Dang day code len nhanh 'develop1' tren Github...
git add PushToDevelop1.bat
git commit -m "add PushToDevelop1 script"
git push -u origin develop1
echo.
echo ===================================================
if %errorlevel% equ 0 (
    echo Thanh cong! Ban hay len Github, vao muc Pull Requests de tao Pull Request gop code tu nhanh develop1 vao nhanh develop nhe.
) else (
    echo Co loi xay ra. Vui long kiem tra lai ket noi hoac quyen truy cap Github.
)
echo ===================================================
pause
