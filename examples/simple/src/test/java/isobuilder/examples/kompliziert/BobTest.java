package isobuilder.examples.kompliziert;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobTest {

  private static final ThreadLocal<String> UPDATER = new ThreadLocal<String>(){
    @Override
    protected String initialValue() {
      return super.initialValue();
    }
  };

  @Test
  public void kevinIsBob() {
    Bob foo = Bob.create("a", "b", "c");
    System.out.println(foo);
    UPDATER.get();
//    BobBuilder.Contract.BobUpdater updater = BobBuilder
//        .kevin("kevin")
//        .chantal("chantal")
//        .justin("justin");
//    assertThat(updater.build(), is(Bob.create("bob", "chantal", "justin")));
//    assertThat(updater.updateKevin("bob").build(), is(Bob.create("bob", "chantal", "justin")));
  }

}