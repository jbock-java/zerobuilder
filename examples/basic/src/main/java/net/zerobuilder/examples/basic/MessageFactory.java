package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.Build.Goal;

@Build
final class MessageFactory {

  final String sender;

  @Goal
  MessageFactory(String sender) {
    this.sender = sender;
  }

  @Goal
  Message create(String body, String recipient, String subject) {
    return MessageFactory_MessageBuilders.messageBuilder()
        .sender(sender)
        .body(body)
        .recipient(recipient)
        .subject(subject);
  }

  @Build(nogc = true)
  static class Message {
    final String sender;
    final String body;
    final String recipient;
    final String subject;

    Message(String sender, String body, String recipient, String subject) {
      this.sender = sender;
      this.body = body;
      this.recipient = recipient;
      this.subject = subject;
    }
  }

  MessageFactoryBuilders.MessageBuilder.Contract.Body messageBuilder() {
    return MessageFactoryBuilders.messageBuilder(this);
  }

}
