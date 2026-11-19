@echo off
chcp 936 >nul
rem 构建 HorizonAdminMonitor。
rem 编译期依赖放在 libs：bukkit-api-1.7.10.jar 取自 Thermos 服务端 jar，
rem protocollib-3.7.jar 取自测试服 plugins 目录，两者都只在编译期使用。

setlocal
set HERE=%~dp0
cd /d "%HERE%"

set API_JAR=%HERE%libs\bukkit-api-1.7.10.jar
set PLIB_JAR=%HERE%libs\protocollib-3.7.jar

if not exist "%API_JAR%" call :fetch_api
if not exist "%PLIB_JAR%" (
    echo [错误] 缺少 libs\protocollib-3.7.jar
    echo        从测试服 plugins 目录复制 ProtocolLib.jar 并改名为 protocollib-3.7.jar
    goto fail
)

echo [构建] mvn clean package
call mvn -B clean package
if errorlevel 1 goto fail

echo.
echo 构建完成：%HERE%target\HorizonAdminMonitor-1.0.0.jar
echo 安装位置：server2\plugins\，重启服务端后生效
goto end

:fetch_api
set URL=https://github.com/GTNewHorizons/Thermos/releases/download/1.0-GTNH/Thermos-1.7.10-1614.UNOFFICIAL-server.jar
set EXPECTED=b02e4c9d6cd2df87aac34f314c35fb1d5995e72c
set JAR_TOOL=jar
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set JAR_TOOL=%JAVA_HOME%\bin\jar.exe

echo [依赖] 提取 Bukkit API
if exist "%HERE%tmp\api" rmdir /s /q "%HERE%tmp\api"
mkdir "%HERE%tmp\api"
if not exist "%HERE%libs" mkdir "%HERE%libs"

curl -L --fail --max-time 300 -o "%HERE%tmp\thermos-server.jar" "%URL%"
if errorlevel 1 (
    echo [错误] 下载失败，请手工放置 libs\bukkit-api-1.7.10.jar
    goto fail
)

set ACTUAL=
for /f "skip=1 delims=" %%H in ('certutil -hashfile "%HERE%tmp\thermos-server.jar" SHA1') do if not defined ACTUAL set "ACTUAL=%%H"
set ACTUAL=%ACTUAL: =%
if /i not "%ACTUAL%"=="%EXPECTED%" (
    echo [错误] SHA-1 校验不匹配，请手工放置 libs\bukkit-api-1.7.10.jar
    goto fail
)

pushd "%HERE%tmp\api"
"%JAR_TOOL%" xf "%HERE%tmp\thermos-server.jar" org/bukkit
"%JAR_TOOL%" cf "%API_JAR%" org
popd
if exist "%HERE%tmp\api" rmdir /s /q "%HERE%tmp\api"
del /q "%HERE%tmp\thermos-server.jar" 2>nul
exit /b 0

:fail
echo.
echo [失败] 构建未完成

:end
endlocal
pause
