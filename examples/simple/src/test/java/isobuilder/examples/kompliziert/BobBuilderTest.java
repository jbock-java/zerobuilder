package isobuilder.examples.kompliziert;

import org.junit.Test;

public class BobBuilderTest {

  @Test
  public void kevin() {
    Bob bob = BobBuilder.builder()
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin")
        .updateKevin("bob")
        .build();
    System.out.println(bob);
  }

}