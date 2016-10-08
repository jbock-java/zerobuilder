package net.zerobuilder.examples.values.inheritance;

import org.junit.Test;

import java.math.BigInteger;

import static net.zerobuilder.examples.values.inheritance.StarBuilders.starBuilder;
import static net.zerobuilder.examples.values.inheritance.StarBuilders.starToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StarTest {

  @Test
  public void test() {
    Star sirius = starBuilder()
        .mass(BigInteger.valueOf(202));
    Star rigel = starToBuilder(sirius)
        .mass(BigInteger.valueOf(2300))
        .build();
    assertThat(sirius.mass, is(BigInteger.valueOf(202)));
    assertThat(rigel.mass, is(BigInteger.valueOf(2300)));
  }
}