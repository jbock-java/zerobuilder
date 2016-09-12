package net.zerobuilder.examples.basic;

import org.junit.Test;

import static net.zerobuilder.examples.basic.BeanBuilders.beanBuilder;
import static net.zerobuilder.examples.basic.BeanBuilders.beanToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BeanTest {

  @Test
  public void testBean() throws Exception {
    Bean bean = beanBuilder()
        .variety("MyBean")
        .height(12)
        .width(10)
        .length(11);
    Bean otherBean = beanToBuilder(bean)
        .variety("YourBean")
        .height(180)
        .build();
    assertThat(bean.getVariety(), is("MyBean"));
    assertThat(bean.getHeight(), is(12l));
    assertThat(bean.getWidth(), is(10l));
    assertThat(bean.getLength(), is(11l));
    assertThat(otherBean.getVariety(), is("YourBean"));
    assertThat(otherBean.getHeight(), is(180l));
    assertThat(otherBean.getWidth(), is(10l));
    assertThat(otherBean.getLength(), is(11l));
  }

}