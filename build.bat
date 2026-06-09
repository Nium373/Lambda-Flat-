@echo off
set TEXT_PARSING=C:\Users\mrniu\Downloads\text-parsing
set SRC=%TEXT_PARSING%\src\main\java

if not exist lib_out mkdir lib_out
if not exist out mkdir out

javac -d lib_out ^
  "%SRC%\com\redcraft\text\Text.java" ^
  "%SRC%\com\redcraft\text\lexer\GreedyLexer.java" ^
  "%SRC%\com\redcraft\text\lexer\LexerException.java" ^
  "%SRC%\com\redcraft\text\lexer\TokenPredicate.java" ^
  "%SRC%\com\redcraft\text\rule\LexerRule.java" ^
  "%SRC%\com\redcraft\text\token\Token.java" ^
  "%SRC%\com\redcraft\text\token\TokenType.java" ^
  "%SRC%\com\redcraft\text\token\SourceInfo.java" ^
  "%SRC%\com\redcraft\text\token\TreeToken.java" ^
  "%SRC%\com\redcraft\text\token\StructuredToken.java"

if %errorlevel% neq 0 exit /b %errorlevel%

javac -cp lib_out -d out src\lambdaflat\*.java

echo.
echo Built. Run with: java -cp lib_out;out lambdaflat.Main test.lambda
