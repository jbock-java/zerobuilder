package net.zerobuilder.examples.derive4j;

import org.junit.Test;

import java.util.Optional;

import static net.zerobuilder.examples.derive4j.Requests.getBody;
import static net.zerobuilder.examples.derive4j.Requests.getPath;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RequestTest {

  @Test
  public void buildPost() throws Exception {
    Request body = RequestBuilders.requestBuilder().path("/").body("Hello world!");
    assertThat(getPath(body), is("/"));
    assertThat(getBody(body), is(Optional.of("Hello world!")));
  }

}