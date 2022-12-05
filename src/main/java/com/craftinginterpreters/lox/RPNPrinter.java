package com.craftinginterpreters.lox;

public class RPNPrinter implements Expr.Visitor<String> {
  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return null;
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return printPolish(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return printPolish("", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return printPolish(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return null;
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return null;
  }

  String print(Expr expr) {
    return expr.accept(this);
  }

  private String printPolish(String operatorName, Expr... exprs) {
    StringBuilder sb = new StringBuilder();
    for (Expr expr : exprs) {
      sb.append(" ");
      sb.append(expr.accept(this));
      sb.append(" ");
    }
    sb.append(operatorName);
    return sb.toString();
  }

  public static void main(String[] args) {
    Expr first_expression =
        new Expr.Binary(
            new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(new Expr.Literal(45.67)));
    System.out.println(new RPNPrinter().print(first_expression));

    Expr second_expression =
        new Expr.Binary(
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal("1"),
                    new Token(TokenType.PLUS, "+", null, 1),
                    new Expr.Literal(2))),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal("4"),
                    new Token(TokenType.MINUS, "-", null, 1),
                    new Expr.Literal(3))));
    System.out.println(new RPNPrinter().print(second_expression));
  }
}
