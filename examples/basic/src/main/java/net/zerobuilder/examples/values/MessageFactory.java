package net.zerobuilder.examples.values;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

// non-static goal
// see MessageFactoryTest
@Builders(recycle = true)
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

  @Builders(recycle = true)
  static final class Message {
    final String sender;
    final String body;
    final String recipient;
    final String subject;

    @Goal(updater = true)
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
