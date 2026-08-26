package net.zerobuilder.examples.values;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.zerobuilder.RecordBuilder;

@RecordBuilder
final class EmptyListConvenience {

  final List things;
  final List<String> strings;
  final Collection<List<String>> collection;
  final Iterable<Collection<List<String>>> iterables;
  final Set<Iterable<Collection<List<String>>>> sets;

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
