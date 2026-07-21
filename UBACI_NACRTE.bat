@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title SOV - Ubaci nacrte u APK
cd /d "%~dp0"

echo ============================================
echo   SOV - konverzija nacrta PNG u WebP
echo   Izvor:  D:\nacrti_png
echo   Cilj:   app\src\main\assets\nacrti_bundled
echo ============================================
echo.

if not exist "D:\nacrti_png" (
    echo GRESKA: Ne postoji D:\nacrti_png - provjeri folder s nacrtima.
    pause
    exit /b 1
)
if not exist "app\src\main\assets" (
    echo GRESKA: Ovaj .bat mora biti u rootu projekta ^(Admin^),
    echo pored convert_nacrti.py - ne vidim app\src\main\assets.
    pause
    exit /b 1
)

rem ---------- Pronadji Python ----------
set "PYEXE="

rem 1. py launcher (standard na Windowsu)
where py >nul 2>nul && (
    py -3 -c "print(1)" >nul 2>nul && set "PYEXE=py -3"
)

rem 2. python na PATH-u
if not defined PYEXE (
    where python >nul 2>nul && (
        python -c "print(1)" >nul 2>nul && set "PYEXE=python"
    )
)

rem 3. tipicne instalacijske lokacije (najnovija verzija prva)
if not defined PYEXE (
    for %%D in (
        "%LOCALAPPDATA%\Programs\Python"
        "C:\Program Files\Python"
        "C:\Program Files (x86)\Python"
        "C:\"
    ) do (
        if not defined PYEXE if exist "%%~D" (
            for /f "delims=" %%P in ('dir /b /ad /o-n "%%~D\Python3*" 2^>nul') do (
                if not defined PYEXE if exist "%%~D\%%P\python.exe" set "PYEXE="%%~D\%%P\python.exe""
            )
        )
    )
)

rem 4. Microsoft Store instalacija
if not defined PYEXE (
    if exist "%LOCALAPPDATA%\Microsoft\WindowsApps\python.exe" (
        "%LOCALAPPDATA%\Microsoft\WindowsApps\python.exe" -c "print(1)" >nul 2>nul && set "PYEXE="%LOCALAPPDATA%\Microsoft\WindowsApps\python.exe""
    )
)

if not defined PYEXE (
    echo GRESKA: Ne mogu naci Python nigdje na racunalu.
    echo Instaliraj s https://www.python.org/downloads/
    echo i oznaci kvacicu "Add python.exe to PATH".
    pause
    exit /b 1
)

echo Koristim Python: !PYEXE!
!PYEXE! --version
echo.

echo Provjera Pillow biblioteke...
!PYEXE! -c "import PIL" 2>nul || (
    echo Instaliram Pillow...
    !PYEXE! -m pip install pillow
)

echo.
echo Konvertiram - ovo moze potrajati nekoliko minuta za 1GB...
echo.
!PYEXE! convert_nacrti.py "D:\nacrti_png" --include-pdf --out "app\src\main\assets\nacrti_bundled"
if errorlevel 1 (
    echo.
    echo GRESKA u konverziji - pogledaj poruke iznad.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   GOTOVO! Nacrti su u assets/nacrti_bundled.
echo   Sad samo: Android Studio - Build - Build APK
echo ============================================
pause
