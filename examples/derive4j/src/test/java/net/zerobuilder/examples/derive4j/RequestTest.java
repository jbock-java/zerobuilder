package net.zerobuilder.examples.derive4j;

import org.junit.Test;

import java.util.Optional;
import java.util.function.Function;

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
    Request body = RequestBuilders.putBuilder()
        .path("/put")
        .body("Hello world!");
    assertThat(getPath(body), is("/put"));
    assertThat(getBody(body), is(Optional.of("Hello world!")));
    assertThat(type.apply(body), is("PUT"));
  }

  @Test
  public void post() throws Exception {
    Request body = RequestBuilders.postBuilder()
        .path("/post")
        .body("<someXmlData/>");
    assertThat(getPath(body), is("/post"));
    assertThat(getBody(body), is(Optional.of("<someXmlData/>")));
    assertThat(type.apply(body), is("POST"));
  }

  @Test
  public void get() throws Exception {
    Request body = RequestBuilders.getBuilder().path("/get");
    assertThat(getPath(body), is("/get"));
    assertThat(getBody(body), is(Optional.empty()));
    assertThat(type.apply(body), is("GET"));
  }

  @Test
  public void delete() throws Exception {
    Request body = RequestBuilders.deleteBuilder().path("/delete");
    assertThat(getPath(body), is("/delete"));
    assertThat(getBody(body), is(Optional.empty()));
    assertThat(type.apply(body), is("DELETE"));
  }

}