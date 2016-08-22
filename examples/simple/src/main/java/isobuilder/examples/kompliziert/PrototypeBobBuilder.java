package isobuilder.examples.kompliziert;

import isobuilder.examples.kompliziert.PrototypeBobBuilder_Contract.BobContract;

import javax.annotation.Generated;

@Generated("prototype")
public class PrototypeBobBuilder implements BobContract {

  private String kevin;
  private String chantal;
  private String justin;

  static PrototypeBobBuilder_Contract.KevinStep builder() {
    return new PrototypeBobBuilder();
  }

  @Override
  public PrototypeBobBuilder_Contract.ChantalStep kevin(String kevin) {
    this.kevin = kevin;
    return this;
  }

  @Override
  public PrototypeBobBuilder_Contract.JustinStep chantal(String chantal) {
    this.chantal = chantal;
    return this;
  }

  @Override
  public PrototypeBobBuilder_Contract.Updater justin(String justin) {
    this.justin = justin;
    return this;
  }

  @Override
  public PrototypeBobBuilder_Contract.Updater updateKevin(String kevin) {
    this.kevin = kevin;
    return this;
  }

  @Override
  public PrototypeBobBuilder_Contract.Updater updateChantal(String chantal) {
    this.chantal = chantal;
    return this;
  }

  @Override
  public PrototypeBobBuilder_Contract.Updater updateJustin(String justin) {
    this.justin = justin;
    return this;
  }

  @Override
  public Bob build() {
    return Bob.createBob(kevin, chantal, justin);
  }

}
