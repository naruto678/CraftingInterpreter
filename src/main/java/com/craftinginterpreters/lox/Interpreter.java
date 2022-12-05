package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    env.assign(expr.name, value);
    return value;
  }
  final Environment globals = new Environment(null);

  private Environment env = globals;

  public Interpreter() {
    globals.define(
        "clock",
        new LoxCallable() {
          @Override
          public Object call(Interpreter interpreter, List<Object> arguments) {
            return (double) System.currentTimeMillis() / 1000;
          }

          @Override
          public int arity() {
            return 0;
          }

          @Override
          public String toString() {
            return "<native fn>";
          }
        });
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return (double) left - (double) right;
      case SLASH:
        checkNumberOperand(expr.operator, left, right);
        checkZeroDivision(expr.operator, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperand(expr.operator, left, right);
        return (double) left * (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (Double) left + (Double) right;
        }
        if (left instanceof String || right instanceof String) {
          return stringify(left) + stringify(right);
        }
        break;
      case GREATER:
        checkNumberOperand(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperand(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double) left <= (double) right;
      case BANG_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return isEqual(left, right);
    }
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes");
    }
    LoxCallable function = (LoxCallable) callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
          expr.paren,
          String.format(
              "Expected %d number of arguments. Got %d", function.arity(), arguments.size()));
    }
    return function.call(this, arguments);
  }

  private void checkZeroDivision(Token operator, Object right) {
    Double val = (Double) right;
    if (val.compareTo(0.0) == 0) throw new RuntimeError(operator, "Division by zero not allowed");
  }

  private void checkNumberOperand(Token operator, Object... right) {
    for (Object val : right) {
      if (val instanceof Double) continue;
      throw new RuntimeError(operator, "Operator must be a number");
    }
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null) return false;
    return left.equals(right);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case MINUS:
        return -(double) right;
      case BANG:
        return !isTruthy(right);
    }
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Object val = env.get(expr.name);
    if (val == null)
      throw new RuntimeError(
          expr.name, String.format("Variable %s might not be initialized", expr.name.lexeme));
    return val;
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt stmt : statements) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private String stringify(Object value) {
    if (value == null) return "nil";
    if (value instanceof Double) {
      String text = value.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return value.toString();
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(expr.right);
  }

  @Override
  public Object visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(env));
    return null;
  }

  private void executeBlock(List<Stmt> statements, Environment environment) {
    Environment prev = this.env;
    try {
      this.env = environment;
      for (Stmt statement : statements) execute(statement);
    } finally {
      this.env = prev;
    }
  }

  @Override
  public Object visitExpressionStmt(Stmt.Expression stmt) {
    return evaluate(stmt.expression);
  }

  @Override
  public Object visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Object visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    env.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }
}
