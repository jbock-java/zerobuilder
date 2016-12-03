package net.zerobuilder.examples.derive4j;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;
import net.zerobuilder.Recycle;
import org.derive4j.Data;

// see RequestTest
@Data
abstract class Request {

  interface Cases<R> {
    R GET(String path);
    R DELETE(String path);
    R PUT(String path, String body);
    R POST(String path, String body);
  }

  abstract <R> R match(Cases<R> cases);

  @Builder
  @Recycle
  @GoalName("get")
  static Request get(String path) {
    return Requests.GET(path);
  }

  @Builder
  @Recycle
  @GoalName("delete")
  static Request delete(String path) {
    return Requests.DELETE(path);
  }

  @Builder
  @Recycle
  @GoalName("put")
  static Request put(String path, String body) {
    return Requests.PUT(path, body);
  }


  @Builder
  @Recycle
  @GoalName("post")
  static Request post(String path, String body) {
    return Requests.POST(path, body);
  }

}
