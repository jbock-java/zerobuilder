package net.zerobuilder.examples.autovalue;

import com.google.auto.value.AutoValue;
import net.zerobuilder.Build;

@AutoValue
@Build(toBuilder = true)
abstract class Bob {

  abstract String kevin();
  abstract String chantal();
  abstract String justin();

  static Bob create(String kevin, String chantal, String justin) {
    return new AutoValue_Bob(kevin, chantal, justin);
  }

  BobBuilder.Contract.BobUpdater toBuilder() {
    return BobBuilder.toBuilder(this);
  }

  Bob withChantal(String chantal) {
    return toBuilder().chantal(chantal).build();
  }

  Bob withKevin(String kevin) {
    return toBuilder().kevin(kevin).build();
  }

  Bob withJustin(String justin) {
    return toBuilder().justin(justin).build();
  }

}
