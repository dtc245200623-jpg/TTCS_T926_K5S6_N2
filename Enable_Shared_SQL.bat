@echo off
chcp 65001 >nul
:: Yêu cầu quyền Quản trị viên (Run as Administrator)
if "%1"=="am_admin" (goto got_admin)
echo Đang yêu cầu quyền Administrator...
echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
echo UAC.ShellExecute "%~s0", "am_admin", "", "runas", 1 >> "%temp%\getadmin.vbs"
"%temp%\getadmin.vbs"
del "%temp%\getadmin.vbs"
exit /B

:got_admin
echo ========================================================
echo  CẤU HÌNH SQL SERVER (SQLEXPRESS02) ĐỂ CHIA SẺ TRONG NHÓM
echo ========================================================
echo.

echo 1. Cố định cổng TCP 1433 cho instance SQLEXPRESS02...
powershell -Command "Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS02\MSSQLServer\SuperSocketNetLib\Tcp\IPAll' -Name 'TcpPort' -Value '1433'; Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS02\MSSQLServer\SuperSocketNetLib\Tcp\IPAll' -Name 'TcpDynamicPorts' -Value ''"

echo 2. Mở cổng trên Windows Firewall (TCP 1433 & UDP 1434)...
netsh advfirewall firewall delete rule name="SQL Server Port 1433" >nul 2>&1
netsh advfirewall firewall add rule name="SQL Server Port 1433" dir=in action=allow protocol=TCP localport=1433 >nul
netsh advfirewall firewall delete rule name="SQL Server Browser Port 1434" >nul 2>&1
netsh advfirewall firewall add rule name="SQL Server Browser Port 1434" dir=in action=allow protocol=UDP localport=1434 >nul

echo 3. Khởi động lại dịch vụ SQL Server và SQL Browser...
powershell -Command "Restart-Service -Name 'MSSQL$SQLEXPRESS02' -Force; Restart-Service -Name 'SQLBrowser' -Force"

echo.
echo ========================================================
echo CẤU HÌNH THÀNH CÔNG!
echo.
echo Địa chỉ IP hiện tại của máy bạn:
powershell -Command "Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -match 'Wi-Fi|Ethernet' -and $_.IPAddress -notlike '169.*' } | Select-Object IPAddress, InterfaceAlias | Format-Table -AutoSize"
echo.
echo Các thành viên trong nhóm có thể điền vào file .env như sau:
echo   DB_SERVER=<IP_o_tren>,1433
echo   DB_NAME=TTCS_T926_K5S6_N2
echo   DB_USER=sa
echo   DB_PASSWORD=123
echo ========================================================
pause
