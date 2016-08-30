The builder pattern can improve code clarity by making parameter names visible at the call site, 
but requires some boilerplate and causes more work for the garbage collector.

In many implementations of the builder pattern, it becomes possible to "forget" a required argument, and typically a default value like `0` or `null` is then used instead.
