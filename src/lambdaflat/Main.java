package lambdaflat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static void runFile(String path) {
        try {
            String source = Files.readString(Paths.get(path));
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            List<Stmt> program = parser.parse();
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(program);
        } catch (IOException e) {
            System.err.println("Error: Could not open file '" + path + "'.");
        } catch (ParseError e) {
            System.err.println("Parse error [line " + e.line + "]: " + e.getMessage());
        } catch (RuntimeError e) {
            System.err.println("Runtime error [line " + e.line + "]: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void repl() {
        Interpreter interpreter = new Interpreter();
        Scanner scanner = new Scanner(System.in);

        System.out.println("\u03BB\u266D v0.1  --  type 'exit' to quit\n");

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();

            if (line.equals("exit") || line.equals(":q")) break;
            if (line.isEmpty()) continue;

            Lexer lexer = new Lexer(line);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            try {
                List<Stmt> program = parser.parse();
                interpreter.interpret(program);
            } catch (ParseError e) {
                System.err.println("Parse error [line " + e.line + "]: " + e.getMessage());
            } catch (RuntimeError e) {
                System.err.println("Runtime error [line " + e.line + "]: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            runFile(args[0]);
        } else {
            repl();
        }
    }
}
