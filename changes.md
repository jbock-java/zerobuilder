### 2016-09-13 zerobuilder 1.201 released

* simplified the generated structure (potentially breaking)

### 2016-09-14 zerobuilder 1.211 released

* added build option, default is true

### 2016-09-17 zerobuilder 1.301 released

* beans: support for setterless collections
* beans: default step order is now alphabetic (breaking) 

### 2016-09-18 zerobuilder 1.311 released

* remove field goal option (breaking)

### 2016-09-18 zerobuilder 1.312 released

* allow inheritance for beans

### 2016-09-18 zerobuilder 1.313 released

* beans: bugfix setterless collection w/generics

### 2016-09-19 zerobuilder 1.314 released

* beans/setterless collection: fix corner case with iterables

### 2016-09-20 zerobuilder 1.321 released

* beans: allow all-caps goal names (breaking)

### 2016-09-22 zerobuilder 1.331 released

* beans: fix generics bug
* `@Step` value now optional
* added null checking via `@Step(nonNull = true)`

### 2016-09-23 zerobuilder 1.401 released

* add goal-level nonNull option
* beans: add @Ignore annotation

### 2016-09-26 zerobuilder 1.411 released

* bugfix: elements of setterless collection should only be null-checked if nullCheck = true
* fields in bean builders are now PRIVATE
* detect and correct possible name collisions of local variables in generated code

### 2016-09-26 zerobuilder 1.412 released

* beans: remove null checking option of collection elements (breaking)

### 2016-09-27 zerobuilder 1.421 released

* beans: remove singleton list shortcut for lone getters (breaking)

### 2016-09-27 zerobuilder 1.431 released

* rename `collection()` to `emptyCollection()` (beans) (lone getters) (breaking)

### 2016-09-29 zerobuilder 1.441 released

* `emptyCollection()` convenience for `java.util.Set` and `java.util.List`

### 2016-10-04 zerobuilder 1.451 released

* allow package-visible beans
* add `builderAccess` and `toBuilderAccess` flags

### 2016-10-06 zerobuilder 1.461 released

* split off zerobuilder-api. api versions will be synchronous

### 2016-10-07 zerobuilder 1.462 released

* fix API inconsistency in RegularParameter.create

### 2016-10-07 zerobuilder 1.463 released

* GoalOptions: access level is now PUBLIC by default (api)

### 2016-10-07 zerobuilder 1.464 released

* remove guava dependency (api)
* add Access.PRIVATE (api)

### 2016-10-07 zerobuilder 1.471 released

* change nonNull attribute to nullPolicy (breaking)
* verify parameter match (api)

### 2016-10-08 zerobuilder 1.472 released

* forbid declared exeptions in accessors and projections (bugfix)
* forbid abstract bean (bean) (bugfix)
* forbid constructor goal in abstract class (bugfix)
* handle overloaded setters correctly (bean) (bugfix)

### 2016-10-09 zerobuilder 1.481 released

* replace booleans with enums (breaking) (api)

### 2016-10-09 zerobuilder 1.482 released

* add DtoProjectionInfo (breaking) (api)
* simplify the return type of Generator.generate (breaking) (api)

### 2016-10-09 zerobuilder 1.483 released

* allow projections to declare exceptions (value)

### 2016-10-13 zerobuilder 1.484 released

* changes to the generated structure: 
  throw UnsupportedOperationException in unused constructors,
  use final fields where possible

### 2016-10-13 zerobuilder 1.501 released

* rename `toBuilder` -> `updater` (breaking)
* rename `build()` -> `done()` (breaking)

### 2016-11-01 zerobuilder 1.511 released

* add ref counter (recycle) (issues-3)
* forbid updater=true for instance-method goal (bugfix)
* modulize the Java API

### 2016-11-03 zerobuilder 1.512 released

* ref tracking via boolean (recycle) (issues-3)
* free memory (recycle) (issues-1)

### 2016-11-03 zerobuilder 1.521 released

* add generics support for instance goals and static goals (issues-2)

### 2016-11-16 zerobuilder 1.522 released

* fix bugs in goals with type parameters
* allow constructor + type parameters (issues-2)

### 2016-11-18 zerobuilder 1.523 released

* allow generic update (issues-2)

### 2016-11-20 zerobuilder 1.524 released

* bugfix: updater was ignoring checked exception
* bugfix: generics + void

### 2016-11-23 zerobuilder 1.525 released

* remove emptyFoo convenience (breaking)

### 2016-11-26 zerobuilder 1.526 released

* bugfix generic instance

### 2016-11-29 zerobuilder 1.531 released

* allow goals with no parameters
* allow update for instance goals

### 2016-11-29 zerobuilder 1.532 released

* instance-update: rename static method fooBuilder -> fooBuilderFactory

### 2016-11-29 zerobuilder 1.541 released

* make @Builders annotation optional

### 2016-12-03 zerobuilder 1.601 released

* major change of the public interface (breaking)
* bugfix generic instance

### 2016-12-03 zerobuilder 1.602 released

* rename @Ignore -> @BeanIgnore (breaking)

### 2016-12-06 zerobuilder 1.603 released

* maintenance release
* update javadoc
* potentially fix a problem where references were not cleared in instance goal (recycle)

### 2016-12-10 zerobuilder 1.604 released

* preserve ordering of typevars (if any)

### 2016-12-14 zerobuilder 1.605 released

* bugfix generic instance

### 2016-12-14 zerobuilder 1.606 released

* bugfixes related to null checking

### 2016-12-16 zerobuilder 1.607 released

* updater: eager null checking in projections

### 2016-12-16 zerobuilder 1.608 released

* updater: null checking only in done method
* rename bean annotations (breaking)

### 2016-12-17 zerobuilder 1.611 released

* rename StepGetter -> GetterOrder (breaking)
