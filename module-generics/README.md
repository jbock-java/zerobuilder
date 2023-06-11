# module-generics

A zerobuilder module that generates an immutable variant of the builder pattern.
All fields in the generated step classes are final. 

Once created, the steps are immutable and thread safe. They may not
be used in conjunction with the `@Recycle` option.

This style of builder is used automatically if the goal method contains any type variables.
It can also be specified explicitly by using `@Builder(style = Style.IMMUTABLE)`.
