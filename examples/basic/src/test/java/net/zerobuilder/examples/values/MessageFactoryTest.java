package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.MessageFactory.Message;
import org.junit.Test;

import static net.zerobuilder.examples.values.MessageFactoryBuilders.messageFactoryBuilder;
import static net.zerobuilder.examples.values.MessageFactory_MessageBuilders.messageToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MessageFactoryTest {

  final MessageFactory messageFactory = messageFactoryBuilder()
      .sender("Alice");

  @Test
  public void message() throws Exception {
    Message message = messageFactory.messageBuilder()
        .body("Hi")
        .recipient("Bob")
        .subject("test");
    assertThat(message.sender, is("Alice"));
    assertThat(message.body, is("Hi"));
    assertThat(message.recipient, is("Bob"));
    assertThat(message.subject, is("test"));
    assertThat(messageToBuilder(message).body("Goodbye").build().body,
        is("Goodbye"));
  }

}