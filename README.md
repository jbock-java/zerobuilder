[![zerobilder-compiler](https://img.shields.io/maven-central/v/io.github.jbock-java/zerobuilder-compiler?label=zerobuilder-compiler)](https://central.sonatype.com/artifact/io.github.jbock-java/zerobuilder-compiler)
[![zerobuilder](https://img.shields.io/maven-central/v/io.github.jbock-java/zerobuilder?label=zerobuilder)](https://central.sonatype.com/artifact/io.github.jbock-java/zerobuilder)

# zerobuilder

This generates a "telescoping" builder where every field must be filled, otherwise it's a compiler error.

To generate the builder, annotate a record class with `@RecordBuilder` and / or `@RecordUpdater`.

```java
@RecordBuilder
record Message(
  String sender,
  String body,
  String recipient
) {
}
```

The generated class will be called `MessageBuilders`.

```java
@Generated
final class MessageBuilders {

  static SenderStep builder()

  static MessageUpdater builder(Message message)

  interface SenderStep { BodyStep sender(String sender) }
  interface BodyStep { RecipientStep body(String body) }
  interface RecipientStep { Message recipient(String recipient) }

  private static class MessageBuilder implements SenderStep, BodyStep, RecipientStep {
    @Override BodyStep sender(String sender)
    @Override RecipientStep body(String body)
    @Override Message recipient(String recipient)
  }

  static final class MessageUpdater {
    MessageUpdater sender(String sender)
    MessageUpdater body(String body)
    MessageUpdater recipient(String recipient)
    Message build()
  }
}
```

# see also

similar project: https://github.com/DanielLiu1123/recordbuilder
