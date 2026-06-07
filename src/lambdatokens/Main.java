package lambdatokens;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java lambdatokens.Main <file.lambda>");
            return;
        }
        try {
            String source = Files.readString(Paths.get(args[0]));
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (IOException e) {
            System.out.println("Error: Could not open file '" + args[0] + "'.");
        }
    }
}
