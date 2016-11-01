package net.zerobuilder.examples.derive4j;

import org.junit.Test;

import java.util.Optional;
import java.util.function.Function;

import static net.zerobuilder.examples.derive4j.RequestBuilders.putBuilder;
import static net.zerobuilder.examples.derive4j.Requests.getBody;
import static net.zerobuilder.examples.derive4j.Requests.getPath;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RequestTest {

  private final Function<Request, String> type = Requests.cases()
      .GET("GET")
      .DELETE("DELETE")
      .PUT("PUT")
      .POST("POST");

  @Test
  public void put() throws Exception {
    Request body = putBuilder()
        .path("/put")
        .body("body");
    assertThat(getPath(body), is("/put"));
    assertThat(getBody(body), is(Optional.of("body")));
    assertThat(type.apply(body), is("PUT"));
  }

  @Test
  public void post() throws Exception {
    Request body = RequestBuilders.postBuilder()
        .path("/post")
        .body("body");
    assertThat(getPath(body), is("/post"));
    assertThat(getBody(body), is(Optional.of("body")));
    assertThat(type.apply(body), is("POST"));
  }

  @Test
  public void get() throws Exception {
    Request body = RequestBuilders.getBuilder()
        .path("/get");
    assertThat(getPath(body), is("/get"));
    assertThat(getBody(body), is(Optional.empty()));
    assertThat(type.apply(body), is("GET"));
  }

  @Test
  public void delete() throws Exception {
    Request body = RequestBuilders.deleteBuilder()
        .path("/delete");
    assertThat(getPath(body), is("/delete"));
    assertThat(getBody(body), is(Optional.empty()));
    assertThat(type.apply(body), is("DELETE"));
  }

  @Test
  public void refs() {
    Request request = putBuilder()
        .path("/external/path")
        .body(getBody(putBuilder()
            .path("/internal/path")
            .body("body"))
            .orElse(""));
    assertThat(getPath(request), is("/external/path"));
  }
}