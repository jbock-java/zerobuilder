package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build
final class MessageFactory {

  private final String target;

  private MessageFactory(String target) {
    this.target = target;
  }

  static MessageFactory target(String target) {
    return new MessageFactory(target);
  }

  @Build.Goal
  Message createJumpNotice(String foxVelocity, String foxColor, String targetState) {
    return new Message(String.format("The %s %s fox jumps over the %s %s.",
        foxVelocity, foxColor, targetState, target));
  }

  static class Message {
    final String body;

    Message(String body) {
      this.body = body;
    }
  }

}
