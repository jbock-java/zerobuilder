package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.MessageFactory.Message;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class MessageFactoryTest {

  private final MessageFactory messageFactory = MessageFactory.sender("zebra");

  @Test
  public void message() throws Exception {
    Message message = messageFactory.messageBuilder()
        .velocity("quick")
        .color("brown")
        .recipient("dog");
    assertThat(message.body, is("The quick brown zebra jumps over the lazy dog."));
  }

}