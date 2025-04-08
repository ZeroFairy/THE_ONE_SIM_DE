set targetdir=target

IF NOT EXIST "%targetdir%" mkdir %targetdir%

setlocal enabledelayedexpansion
set SOURCES=
for /R src\routing %%f in (*.java) do (
    set SOURCES=!SOURCES! %%f
)

javac -sourcepath src -d %targetdir% -cp lib/ECLA.jar;lib/DTNConsoleConnection.jar;lib/jFuzzyLogic.jar;lib/uncommons-maths-1.2.1.jar;lib/lombok.jar;lib/fastjson-1.2.7.jar src/core/*.java src/reinforcement/actionselection/*.java src/reinforcement/models/*.java src/reinforcement/qlearn/*.java src/reinforcement/utils/*.java src/movement/*.java src/report/*.java !SOURCES! src/gui/*.java src/input/*.java src/applications/*.java src/interfaces/*.java
endlocal

