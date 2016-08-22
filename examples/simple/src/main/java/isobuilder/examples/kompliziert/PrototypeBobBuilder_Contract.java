package isobuilder.examples.kompliziert;

import javax.annotation.Generated;

@Generated("prototype")
public interface PrototypeBobBuilder_Contract {
  interface KevinStep {
    ChantalStep kevin(String kevin);
  }

  interface ChantalStep {
    JustinStep chantal(String chantal);
  }

  interface JustinStep {
    Updater justin(String justin);
  }

  interface Updater {
    Updater updateKevin(String kevin);
    Updater updateChantal(String chantal);
    Updater updateJustin(String justin);
    Bob build();
  }

  interface BobContract extends Updater, ChantalStep, JustinStep, KevinStep {
  }

}
