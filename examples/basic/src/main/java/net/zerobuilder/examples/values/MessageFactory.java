package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.Recycle;
import net.zerobuilder.Updater;

// non-static goal
// see MessageFactoryTest
final class MessageFactory {

  final String sender;

  @Builder
  @Recycle
  MessageFactory(String sender) {
    this.sender = sender;
  }

  @Builder
  @Recycle
  Message create(String body, String recipient, String subject) {
    return MessageFactory_MessageBuilders.messageBuilder()
        .sender(sender)
        .body(body)
        .recipient(recipient)
        .subject(subject);
  }

  static final class Message {
    final String sender;
    final String body;
    final String recipient;
    final String subject;

    @Builder
    @Updater
    @Recycle
    Message(String sender, String body, String recipient, String subject) {
      this.sender = sender;
      this.body = body;
      this.recipient = recipient;
      this.subject = subject;
    }
  }

  MessageFactoryBuilders.MessageBuilder.Body messageBuilder() {
    return MessageFactoryBuilders.messageBuilder(this);
  }
}
