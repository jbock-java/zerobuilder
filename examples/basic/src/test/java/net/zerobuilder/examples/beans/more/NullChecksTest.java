package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.NullChecks.Default;
import net.zerobuilder.examples.beans.more.NullChecks.NullableElements;
import net.zerobuilder.examples.beans.more.NullChecks.UncheckedCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static java.util.Collections.nCopies;
import static net.zerobuilder.examples.beans.more.NullChecks_CheckedCollectionBuilders.checkedCollectionBuilder;
import static net.zerobuilder.examples.beans.more.NullChecks_CheckedStringBuilders.checkedStringBuilder;
import static net.zerobuilder.examples.beans.more.NullChecks_DefaultBuilders.DefaultBuilder;
import static net.zerobuilder.examples.beans.more.NullChecks_NullableElementsBuilders.nullableElementsBuilder;
import static net.zerobuilder.examples.beans.more.NullChecks_UncheckedCollectionBuilders.uncheckedCollectionBuilder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class NullChecksTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void nullElementRejected() {
    thrown.expectMessage("strings");
    String nothing = null;
    checkedCollectionBuilder().strings(nothing);
  }

  @Test
  public void nullableElement() {
    List<String> wrappedNull = nCopies(1, null);
    NullableElements bean = nullableElementsBuilder().strings(wrappedNull);
    assertThat(bean.getStrings().size(), is(1));
    assertThat(bean.getStrings().get(0), is(nullValue()));
  }

  @Test
  public void nullElementAllowed() {
    List<String> wrappedNull = nCopies(1, null);
    UncheckedCollection bean = uncheckedCollectionBuilder().strings(wrappedNull);
    assertThat(bean.getStrings().size(), is(1));
    assertThat(bean.getStrings().get(0), is(nullValue()));
  }

  @Test
  public void collectionMayNotBeNull() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("strings");
    uncheckedCollectionBuilder().strings(null);
  }

  @Test
  public void simpleNullRejected() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("string");
    checkedStringBuilder().string(null);
  }

  @Test
  public void goalLevelNullOk() {
    Default bar = DefaultBuilder().bar("bar").foo(null);
    assertThat(bar.getFoo(), is(nullValue()));
    assertThat(bar.getBar(), is("bar"));
  }

  @Test
  public void goalLevelNullRejected() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("bar");
    DefaultBuilder().bar(null);
  }

}