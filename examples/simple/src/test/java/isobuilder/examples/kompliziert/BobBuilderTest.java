package isobuilder.examples.kompliziert;

import org.junit.Test;

public class BobBuilderTest {

  @Test
  public void kevin() {
    Bob bob = PrototypeBobBuilder.builder()
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin")
        .updateKevin("bob")
        .build();
    System.out.println(bob);
  }

}