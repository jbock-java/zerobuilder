package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.MessageFactory.Message;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class MessageFactoryTest {

  @Test
  public void createJumpNotice() throws Exception {
    MessageFactory messageFactory = MessageFactory.target("dog");
    Message message = MessageFactoryBuilder.builder(messageFactory)
        .foxVelocity("quick")
        .foxColor("brown")
        .targetState("lazy");
    assertThat(message.body, is("The quick brown fox jumps over the lazy dog."));
  }

}