package isobuilder.examples.kompliziert;

import com.google.auto.value.AutoValue;
import com.kaputtjars.isobuilder.Builder;

@AutoValue
abstract class Bob {

  abstract String kevin();
  abstract String chantal();
  abstract String justin();

  @Builder
  static Bob create(String kevin, String chantal, String justin) {
    return new AutoValue_Bob(kevin, chantal, justin);
  }

  BobBuilder.Contract.BobUpdater toBuilder() {
    return BobBuilder
        .kevin(kevin())
        .chantal(chantal())
        .justin(justin());
  }

}
