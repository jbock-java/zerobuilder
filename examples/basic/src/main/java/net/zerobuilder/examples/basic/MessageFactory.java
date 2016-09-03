package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build
final class MessageFactory {

  private final String sender;

  private MessageFactory(String sender) {
    this.sender = sender;
  }

  static MessageFactory sender(String sender) {
    return new MessageFactory(sender);
  }

  @Build.Goal
  Message createJumpNotice(String velocity, String color, String recipient) {
    return new Message(String.format("The %s %s %s jumps over the lazy %s.",
        velocity, color, sender, recipient));
  }

  static class Message {
    final String body;

    Message(String body) {
      this.body = body;
    }
  }

  MessageFactoryBuilders.MessageBuilder.Contract.Velocity messageBuilder() {
    return MessageFactoryBuilders.messageBuilder(this);
  }

}
