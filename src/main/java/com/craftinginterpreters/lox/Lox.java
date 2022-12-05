package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  private static boolean hadError = false;
  private static String version_number = "0.0.1";
  private static boolean hadRuntimeError = false;
  private static final Interpreter interpreter = new Interpreter();

  public static void main(String args[]) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  public static void runFile(String fileName) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(fileName));
    run(new String(bytes));
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void runPrompt() {
    System.out.println("Repl Lox Version " + version_number);
    try (InputStreamReader reader = new InputStreamReader(System.in)) {
      BufferedReader br = new BufferedReader(reader);
      for (; ; ) {
        System.out.print("> ");
        String line = br.readLine();
        if (line == null) {
          break;
        }
        run(line);
        hadError = false;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();
    if (hadError) return;
    interpreter.interpret(statements);
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void report(int line, String where, String message) {
    System.err.println(String.format("[line %d] Error:  %s : %s ",line, where, message));
    hadError = true;
  }

  public static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + " [line " + error.token.line + "]");
    hadRuntimeError = true;
  }
}
