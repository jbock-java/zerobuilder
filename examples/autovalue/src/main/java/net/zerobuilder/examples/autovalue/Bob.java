package net.zerobuilder.examples.autovalue;

import com.google.auto.value.AutoValue;
import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import static net.zerobuilder.examples.autovalue.BobBuilders.bobUpdater;

// see BobTest
@AutoValue
abstract class Bob {

  abstract String kevin();
  abstract String chantal();
  abstract String justin();

  @Builder
  @Updater
  static Bob create(String kevin, String chantal, String justin) {
    return new AutoValue_Bob(kevin, chantal, justin);
  }

  BobBuilders.BobUpdater updater() {
    return bobUpdater(this);
  }

  Bob withChantal(String chantal) {
    return updater().chantal(chantal).done();
  }

  Bob withKevin(String kevin) {
    return updater().kevin(kevin).done();
  }

  Bob withJustin(String justin) {
    return updater().justin(justin).done();
  }

}
