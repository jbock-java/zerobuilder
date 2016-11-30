# zerobuilder for value types

### Motivation

Immutable types, also known as values, are quite popular nowadays.
Their constructors tend to have a lot of arguments, which can lead to code that's hard to read.

Replacing the constructor with a classic builder pattern may improve things, but it comes at a price:

* Creating the builder class is too much work, and it has to be kept in sync.
* Even worse, the builder makes it possible to "forget" a constructor argument. 
  This is especially a problem when existing code still compiles after an argument is added to the constructor.

Zerobuilder takes care of the boilerplate by generating two different variants of the builder pattern:

* A `Builder` to create new instance. 
  In this [variant of the builder pattern][1], it is impossible to omit a constructor argument.
  This is generated once per `@Goal`, unless `@Goal(builder = false)`.
* A classical-builder style `Updater` to make modified shallow copies. 
  This is generated for each goal where `@Goal(updater = true)`.

[1]: http://blog.crisp.se/2013/10/09/perlundholm/another-builder-pattern-for-java

### Builder

Start by adding the `@Goal` annotation to a constructor:

````java
final class Message {

  final String sender;
  final String body;
  final String recipient;

  @Goal
  Message(String sender, String body, String recipient) {
    this.sender = sender;
    this.body = body;
    this.recipient = recipient;
  }
}

````

The following class will be generated:

````java
@Generated final class MessageBuilders {

  static MessageBuilder.Sender messageBuilder() { ... }

  static final class MessageBuilder {
    interface Sender { Body sender(String sender); }
    interface Body { Recipient body(String body); }
    interface Recipient { Message recipient(String recipient); }
  }
}
````

The implementations of the step interfaces `MessageBuilder.Sender`, `MessageBuilder.Body`, `MessageBuilder.Recipient` 
are omitted here.
If you clone this project and do a `mvn install`, you will find the complete source code of `MessageBuilders.java`
in the `examples/basic/target/generated-sources/annotations` folder.

The `messageBuilder` method returns `MessageBuilder.Sender`, the first step of a linear
chain towards the constructor goal:

![steps](https://raw.githubusercontent.com/h908714124/zerobuilder/09642ec5a9ebe2b84ef16930f64f919cb57f9e6e/dot/graph.png "steps diagram")

By default, these steps are in the original order of the goal arguments.
If for some reason you would like to call them in a different order, you can use the `@Step` annotation:

````java
  @Goal
  Message(@Step(1) String sender, 
          @Step(2) String body, 
          @Step(0) String recipient) {
    this.sender = sender;
    this.body = body;
    this.recipient = recipient;
  }

````

### Updater

This time, we override a default by specifying `builder = false`, so that we can study the generated updater in isolation.

````java
final class Message {

  final String sender;
  final String body;
  final String recipient;

  @Goal(updater = true, builder = false)
  Message(String sender, String body, String recipient) {
    this.sender = sender;
    this.body = body;
    this.recipient = recipient;
  }
}

````

In order for the `updater = true` option to make sense,
there has to exist one corresponding _projection_, i.e. a getter or instance field of the same name, 
for each goal parameter.
This is the case in `Message.java`.

The following class will be generated:

````java
@Generated final class MessageBuilders {

  static MessageUpdater messageUpdater(Message message) { ... }

  static final class MessageUpdater {
    MessageUpdater sender(String sender) { ... }
    MessageUpdater body(String body) { ... }
    MessageUpdater recipient(String recipient) { ... }
    Message done() { ... }
  }
}
````

Note that this is the "classic" builder pattern, with no implicit ordering of the steps.

### Factory methods

In addition to constructors, the `@Goal` annotation can appear on methods, even non-static ones. 
Have a look at the [MessageFactory](../master/examples/basic/src/main/java/net/zerobuilder/examples/values/MessageFactory.java) example,
to see what this can be used for.

### Other frameworks

Zerobuilder does not help with the <em>definition</em> of your data types.
Other source generators, like 
[auto-value](https://github.com/google/auto/tree/master/value) 
can be used for the data definition, with zerobuilder handling the builders.

See the [auto-value](../master/examples/autovalue/src/main/java/net/zerobuilder/examples/autovalue/Bob.java) and 
and [derive4j](../master/examples/derive4j/src/main/java/net/zerobuilder/examples/derive4j/Request.java) examples.

Note: auto-value's `abstract property()` methods are valid projections (see above),
so `updater = true` is possible.

### Null checking

Run-time null checks can be added for all non-primitive properties,
by using the goal level `nullPolicy` option:

````java
@Goal(nullPolicy = NullPolicy.REJECT)
public MyConstructor(String required) {
  this.name = required;
}
````

Additionaly, `nullPolicy` can be specified for each individual step:

````java
@Goal(nullPolicy = REJECT)
public MyConstructor(String required,
                     @Step(nullPolicy = ALLOW) String optional) {
  this.required = required;
  this.optional = optional;
}
````

A `nullPolicy` setting on the step level overrides the goal level setting, if any.
The default behaviour is `NullPolicy.ALLOW`.

### Access level

By default, the generated static methods `fooBuilder` and `fooUpdater` are public.
You can change this to default (package) visibility using `@Builders(access = AccessLevel.PACKAGE)`.

Each goal can override this setting with `@Goal(builderAccess)` and `@Goal(updaterAccess)`.
