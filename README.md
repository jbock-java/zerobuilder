The builder pattern can improve code clarity by making parameter names visible where they are used,
but requires some boilerplate and causes more work for the garbage collector.

In many implementations of the builder pattern, it is possible to specify an argument twice, or forget a required argument.
In the latter case, an implicit default value of `null` is then typically used instead of the missing argument.
