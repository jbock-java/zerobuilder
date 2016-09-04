package net.zerobuilder.examples.derive4j;

import net.zerobuilder.Build;
import org.derive4j.Data;

import net.zerobuilder.Goal;

@Data
@Build(nogc = true)
abstract class Request {

  interface Cases<R> {
    R GET(String path);
    R DELETE(String path);
    R PUT(String path, String body);
    R POST(String path, String body);
  }

  abstract <R> R match(Cases<R> cases);

  @Goal(name = "get")
  static Request get(String path) {
    return Requests.GET(path);
  }

  @Goal(name = "delete")
  static Request delete(String path) {
    return Requests.DELETE(path);
  }

  @Goal(name = "put")
  static Request put(String path, String body) {
    return Requests.PUT(path, body);
  }

  @Goal(name = "post")
  static Request post(String path, String body) {
    return Requests.POST(path, body);
  }

}
