package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.MessageFactory.Message;
import org.junit.Test;

import static net.zerobuilder.examples.basic.MessageFactoryBuilders.messageFactoryBuilder;
import static net.zerobuilder.examples.basic.MessageFactory_MessageBuilders.messageToBuilder;
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