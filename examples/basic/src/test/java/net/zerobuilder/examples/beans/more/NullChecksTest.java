package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.NullChecks.UncheckedCollection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.nCopies;
import static net.zerobuilder.examples.beans.more.NullChecks_CheckedCollectionBuilders.checkedCollectionBuilder;
import static net.zerobuilder.examples.beans.more.NullChecks_CheckedStringBuilders.checkedStringBuilder;
import static net.zerobuilder.examples.beans.more.NullChecks_UncheckedCollectionBuilders.uncheckedCollectionBuilder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class NullChecksTest {

  @Test(expected = NullPointerException.class)
  public void nullElementRejected() {
    List<String> wrappedNull = nCopies(1, null);
    checkedCollectionBuilder().strings(wrappedNull);
  }

  @Test
  public void nullElementAllowed() {
    List<String> wrappedNull = nCopies(1, null);
    UncheckedCollection bean = uncheckedCollectionBuilder().strings(wrappedNull);
    assertThat(bean.getStrings().size(), is(1));
    assertThat(bean.getStrings().get(0), is(nullValue()));
  }

  @Test(expected = NullPointerException.class)
  public void collectionMayNotBeNull() {
    uncheckedCollectionBuilder().strings(null);
  }

  @Test(expected = NullPointerException.class)
  public void simpleNullRejected() {
    checkedStringBuilder().string(null);
  }
}