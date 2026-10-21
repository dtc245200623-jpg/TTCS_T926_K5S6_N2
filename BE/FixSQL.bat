@echo off
:: Request Admin privileges
if "%1"=="am_admin" (goto got_admin)
echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
echo UAC.ShellExecute "%~s0", "am_admin", "", "runas", 1 >> "%temp%\getadmin.vbs"
"%temp%\getadmin.vbs"
del "%temp%\getadmin.vbs"
exit /B

:got_admin
echo Dang tien hanh bat TCP/IP cho SQLEXPRESS02...
powershell -Command "$p = Get-WmiObject -Namespace 'root\Microsoft\SqlServer\ComputerManagement17' -Class ServerNetworkProtocol -Filter 'InstanceName=''SQLEXPRESS02'' and ProtocolName=''Tcp'''; $p.SetEnable(); Restart-Service -Name 'MSSQL$SQLEXPRESS02' -Force"
echo Da bat TCP/IP va Restart SQL Server thanh cong! 
echo Ban co the tat cua so nay.
pause
