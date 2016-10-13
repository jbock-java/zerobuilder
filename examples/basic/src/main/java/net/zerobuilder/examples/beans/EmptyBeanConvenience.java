package net.zerobuilder.examples.beans;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Builders
@Goal(updater = true)
public class EmptyBeanConvenience {

  private List things;
  private List<String> strings;
  private Collection<List<String>> collection;
  private Iterable<Collection<List<String>>> iterables;
  private Set<Iterable<Collection<List<String>>>> sets;

  public List getThings() {
    return things;
  }

  public void setThings(List things) {
    this.things = things;
  }

  public List<String> getStrings() {
    return strings;
  }

  public void setStrings(List<String> strings) {
    this.strings = strings;
  }

  public Collection<List<String>> getCollection() {
    return collection;
  }

  public void setCollection(Collection<List<String>> collection) {
    this.collection = collection;
  }

  public Iterable<Collection<List<String>>> getIterables() {
    return iterables;
  }

  public void setIterables(Iterable<Collection<List<String>>> iterables) {
    this.iterables = iterables;
  }

  public Set<Iterable<Collection<List<String>>>> getSets() {
    return sets;
  }

  public void setSets(Set<Iterable<Collection<List<String>>>> sets) {
    this.sets = sets;
  }
}
