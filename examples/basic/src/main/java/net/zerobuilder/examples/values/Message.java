package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.Recycle;
import net.zerobuilder.RejectNull;
import net.zerobuilder.Updater;

// direct field access
// see MessageTest
final class Message {

  final String sender;
  final String body;
  final String recipient;
  final String subject;

  @Builder
  @Updater
  @Recycle
  Message(@RejectNull String sender,
          @RejectNull String body,
          @RejectNull String recipient,
          @RejectNull String subject) {
    this.sender = sender;
    this.body = body;
    this.recipient = recipient;
    this.subject = subject;
  }
}
