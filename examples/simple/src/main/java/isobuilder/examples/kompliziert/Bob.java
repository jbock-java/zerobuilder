package isobuilder.examples.kompliziert;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class Bob {

  abstract String kevin();
  abstract String chantal();
  abstract String justin();

  static AutoValue_Bob create(String kevin, String chantal, String justin) {
    return new AutoValue_Bob(kevin, chantal, justin);
  }

//  BobBuilder.Contract.BobUpdater toBuilder() {
//    return BobBuilder
//        .kevin(kevin())
//        .chantal(chantal())
//        .justin(justin());
//  }

}
