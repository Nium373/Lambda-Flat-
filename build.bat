@echo off
if not exist out mkdir out
javac -d out src\lambdatokens\*.java
if %errorlevel% equ 0 echo Built. Run with: java -cp out lambdatokens.Main test.lambda
