package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.Goal;

@Build(recycle = true)
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

  @Build(recycle = true)
  static final class Message {
    final String sender;
    final String body;
    final String recipient;
    final String subject;

    @Goal(toBuilder = true)
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
