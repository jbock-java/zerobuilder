package net.zerobuilder.examples.values;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Builders
final class EmptyListConvenience {

  final List things;
  final List<String> strings;
  final Collection<List<String>> collection;
  final Iterable<Collection<List<String>>> iterables;
  final Set<Iterable<Collection<List<String>>>> sets;

  @Goal(updater = true)
  EmptyListConvenience(List things,
                       List<String> strings,
                       Collection<List<String>> collection,
                       Iterable<Collection<List<String>>> iterables,
                       Set<Iterable<Collection<List<String>>>> sets) {
    this.things = things;
    this.strings = strings;
    this.collection = collection;
    this.iterables = iterables;
    this.sets = sets;
  }
}
