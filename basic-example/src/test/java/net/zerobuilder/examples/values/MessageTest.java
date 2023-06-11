package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.MessageBuilders.messageBuilder;
import static net.zerobuilder.examples.values.MessageBuilders.messageUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTest {

  @Test
  public void message() throws Exception {
    Message message = messageBuilder()
        .sender("Alice")
        .body("Hi")
        .recipient("Bob")
        .subject("test");
    assertEquals("Alice", message.sender);
    assertEquals("Hi", message.body);
    assertEquals("Bob", message.recipient);
    assertEquals("test", message.subject);
    message = messageUpdater(message)
        .body("Goodbye")
        .done();
    assertEquals("Goodbye", message.body);
  }
}
