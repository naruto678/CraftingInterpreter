package com.craftinginterpreters.lox;

public class RuntimeError extends RuntimeException {
  final Token token;

  public RuntimeError(Token operator, String message) {
    super(message);
    this.token = operator;
  }
}
