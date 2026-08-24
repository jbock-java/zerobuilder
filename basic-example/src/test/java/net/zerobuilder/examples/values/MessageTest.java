package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTest {

  @Test
  void message() {
    Message message = MessageBuilders.builder()
        .sender("Alice")
        .body("Hi")
        .recipient("Bob")
        .subject("test");
    assertEquals("Alice", message.sender);
    assertEquals("Hi", message.body);
    assertEquals("Bob", message.recipient);
    assertEquals("test", message.subject);
    message = MessageBuilders.builder(message)
        .body("Goodbye")
        .build();
    assertEquals("Goodbye", message.body);
  }
}
