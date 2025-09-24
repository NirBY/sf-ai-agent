package com.nby.agent.llm;

public interface LlmProvider {
  double[] embed(String text);
  String chat(String system, String user, int tokens);
}