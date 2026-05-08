@echo off
set JAVA_HOME=C:\Users\nagib\AppData\Local\Java\jdk-17
set ANDROID_HOME=C:\Users\nagib\AppData\Local\Android\Sdk
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d "c:\Users\nagib\Desktop\test\keyboard"

echo Downloading Gradle wrapper...
"C:\Users\nagib\AppData\Local\Java\jdk-17\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar --version 2>nul || (
    echo Creating Gradle wrapper...
    mkdir gradle\wrapper 2>nul
    curl -L "https://services.gradle.org/distributions/gradle-8.2-bin.zip" -o "%TEMP%\gradle.zip"
    powershell -Command "Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath 'C:\Users\nagib\AppData\Local\Temp\gradle' -Force"
    copy "C:\Users\nagib\AppData\Local\Temp\gradle\gradle-8.2\lib\plugins\gradle-wrapper-8.2.jar" "gradle\wrapper\gradle-wrapper.jar" 2>nul
)

echo Building APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo APK built successfully!
    dir "app\build\outputs\apk\debug\*.apk"
) else (
    echo Build failed
    exit /b %ERRORLEVEL%
)
