package isobuilder.examples.kompliziert;

import com.google.auto.value.AutoValue;
import com.kaputtjars.isobuilder.Build;

@AutoValue
@Build
abstract class Bob {

  abstract String kevin();
  abstract String chantal();
  abstract String justin();

  @Build.From
  static Bob create(String kevin, String chantal, String justin) {
    return new AutoValue_Bob(kevin, chantal, justin);
  }

}
