package isobuilder.examples.kompliziert;

import com.google.auto.value.AutoValue;
import isobuilder.Builder;

@AutoValue
abstract class Bob {

  abstract String kevin();
  abstract String chantal();
  abstract String justin();

  @Builder
  static Bob createBob(String kevin, String chantal, String justin) {
    return new AutoValue_Bob(kevin, chantal, justin);
  }

  static Bob makeBob() {
    return null;
  }

}
