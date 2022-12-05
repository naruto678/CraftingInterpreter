package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output_directory> ");
      System.exit(64);
    }
    String outputDir = args[0];
    defineAst(
        outputDir,
        "Expr",
        Arrays.asList(
            "Assign : Token name, Expr value",
            "Binary : Expr left, Token operator, Expr right",
            "Call : Expr callee, Token paren, List<Expr> arguments",
            "Grouping : Expr expression",
            "Literal : Object value",
            "Unary : Token operator, Expr right",
            "Logical : Expr left, Token operator, Expr right",
            "Variable : Token name"));
    defineAst(
        outputDir,
        "Stmt",
        Arrays.asList(
            "Block : List<Stmt> statements",
            "Expression : Expr expression",
            "Print : Expr expression",
            "Var : Token name, Expr initializer",
            "If : Expr condition , Stmt thenBranch , Stmt elseBranch",
            "While : Expr condition, Stmt body"));
  }

  private static void defineAst(String outputDir, String baseName, List<String> types)
      throws IOException {
    String path = String.join("/", outputDir, baseName) + ".java";
    PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);
    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");
    defineVisitor(writer, baseName, types);
    // the AST classes
    for (String type : types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, className, fields, baseName);
    }
    writer.println();
    writer.println("    abstract <R> R accept(Visitor<R> visitor);");
    writer.println("}");
    writer.close();
  }

  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println(
          String.format(
              "    R visit%s(%s %s);", typeName + baseName, typeName, baseName.toLowerCase()));
    }
    writer.println("  }");
  }

  private static void defineType(
      PrintWriter writer, String className, String fields, String baseName) {

    writer.println(String.format("  static class %s extends %s {", className, baseName));
    writer.println(String.format("    %s(%s){", className, fields));
    for (String param : fields.split(",")) {
      param = param.trim();
      String name = param.split("\\s+")[1];
      writer.println(String.format("      this.%s = %s;", name, name));
    }

    writer.println("    }");
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println(String.format("      return visitor.visit%s(this);", className + baseName));
    writer.println("    }");
    // fields
    for (String param : fields.split(",")) {
      writer.println(String.format("    final %s;", param));
    }
    writer.println("  }");
  }
}
