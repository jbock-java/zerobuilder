package net.zerobuilder.examples.values;

import org.junit.Test;

import static net.zerobuilder.examples.values.MessageBuilders.messageBuilder;
import static net.zerobuilder.examples.values.MessageBuilders.messageUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MessageTest {

  @Test
  public void message() throws Exception {
    Message message = messageBuilder()
        .sender("Alice")
        .body("Hi")
        .recipient("Bob")
        .subject("test");
    assertThat(message.sender, is("Alice"));
    assertThat(message.body, is("Hi"));
    assertThat(message.recipient, is("Bob"));
    assertThat(message.subject, is("test"));
    message = messageUpdater(message)
        .body("Goodbye")
        .done();
    assertThat(message.body, is("Goodbye"));
  }

  @Test(expected = NullPointerException.class)
  public void nullBuilder() throws Exception {
    messageBuilder()
        .sender(null)
        .body("Hi")
        .recipient("Bob")
        .subject("test");
  }

  @Test(expected = NullPointerException.class)
  public void nullUpdater() throws Exception {
    Message message = messageBuilder()
        .sender("Alice")
        .body("Hi")
        .recipient("Bob")
        .subject("test");
    messageUpdater(message)
        .body(null)
        .done();
  }


}