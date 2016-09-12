package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

// see BeanTest
@Builders(recycle = true)
@Goal(toBuilder = true)
public class Bean {

  private String variety;
  private long height;
  private long width;
  private long length;

  public String getVariety() {
    return variety;
  }
  public void setVariety(String variety) {
    this.variety = variety;
  }
  public long getHeight() {
    return height;
  }
  public void setHeight(long height) {
    this.height = height;
  }
  public long getWidth() {
    return width;
  }
  public void setWidth(long width) {
    this.width = width;
  }
  public long getLength() {
    return length;
  }
  public void setLength(long length) {
    this.length = length;
  }
}
