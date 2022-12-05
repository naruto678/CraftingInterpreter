package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
  private final Map<String, Object> map = new HashMap<>();
  private Environment enclosing;

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String name, Object value) {
    map.put(name, value);
  }

  Object get(Token name) {
    if (map.containsKey(name.lexeme)) return map.get(name.lexeme);
    if (enclosing != null) return enclosing.get(name);

    throw new RuntimeError(name, String.format("Undefined variable '%s'", name.lexeme));
  }

  public void assign(Token name, Object value) {
    if (map.containsKey(name.lexeme)) {
      map.put(name.lexeme, value);
      return;
    }
    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }
    throw new RuntimeError(name, String.format("Undefined variable '%s'", name.lexeme));
  }
}
