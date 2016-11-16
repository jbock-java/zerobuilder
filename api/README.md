# zerobuilder-api

A library of the core functionality of zerobuilder.
It is intended for use in other annotation processors.

### Caution

Work in progress!

### Usage

See tests in [module-builder](../modules/module-builder), [module-updater](../modules/module-updater) etc.

### State of affairs

This project contains mostly data type definitions, for the modules to work with.
Some technical debt has been piling up near `GoalContextFactory`. 
Also, there are probably <em>too many</em> types.

### Maven

````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder-api</artifactId>
    <version>1.522</version>
</dependency>
````
