package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.MessageFactory.Message;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.MessageFactoryBuilders.messageFactoryBuilder;
import static net.zerobuilder.examples.values.MessageFactory_MessageBuilders.messageUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageFactoryTest {

  final MessageFactory messageFactory = messageFactoryBuilder()
      .sender("Alice");

  @Test
  public void message() {
    Message message = messageFactory.messageBuilder()
        .body("Hi")
        .recipient("Bob")
        .subject("test");
    assertEquals("Alice", message.sender);
    assertEquals("Hi", message.body);
    assertEquals("Bob", message.recipient);
    assertEquals("test", message.subject);
    assertEquals("Goodbye",
        messageUpdater(message).body("Goodbye").done().body);
  }

}
