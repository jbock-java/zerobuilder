package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Goal;

@Builder(recycle = true)
final class Message {

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
