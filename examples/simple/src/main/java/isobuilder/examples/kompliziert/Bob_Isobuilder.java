package isobuilder.examples.kompliziert;

import isobuilder.examples.kompliziert.Bob_Isobuilder.PrototypeBobBuilderContract.BobChantal;
import isobuilder.examples.kompliziert.Bob_Isobuilder.PrototypeBobBuilderContract.BobJustin;
import isobuilder.examples.kompliziert.Bob_Isobuilder.PrototypeBobBuilderContract.BobKevin;
import isobuilder.examples.kompliziert.Bob_Isobuilder.PrototypeBobBuilderContract.BobUpdater;

import javax.annotation.Generated;

@Generated("prototype")
public class Bob_Isobuilder {

  static BobKevin builder() {
    return new PrototypeBobBuilder();
  }

  static final class PrototypeBobBuilder implements BobUpdater, BobChantal, BobJustin, BobKevin {

    private String kevin;
    private String chantal;
    private String justin;

    @Override
    public BobChantal kevin(String kevin) {
      this.kevin = kevin;
      return this;
    }

    @Override
    public BobJustin chantal(String chantal) {
      this.chantal = chantal;
      return this;
    }

    @Override
    public BobUpdater justin(String justin) {
      this.justin = justin;
      return this;
    }

    @Override
    public BobUpdater updateKevin(String kevin) {
      this.kevin = kevin;
      return this;
    }

    @Override
    public BobUpdater updateChantal(String chantal) {
      this.chantal = chantal;
      return this;
    }

    @Override
    public BobUpdater updateJustin(String justin) {
      this.justin = justin;
      return this;
    }

    @Override
    public Bob build() {
      return Bob.createBob(kevin, chantal, justin);
    }

  }

  public static interface PrototypeBobBuilderContract {
    interface BobKevin {
      BobChantal kevin(String kevin);
    }

    interface BobChantal {
      BobJustin chantal(String chantal);
    }

    interface BobJustin {
      BobUpdater justin(String justin);
    }

    interface BobUpdater {
      BobUpdater updateKevin(String kevin);
      BobUpdater updateChantal(String chantal);
      BobUpdater updateJustin(String justin);
      Bob build();
    }

  }
}
