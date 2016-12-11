package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import java.util.Map;

// originally generated by RandomGenericsTest
final class Gen0<A, B extends A> {

  private final A aa0;
  private final A aa1;
  private final A aa2;
  private final B ab0;

  Gen0(A aa0, A aa1, A aa2, B ab0) {
    this.aa0 = aa0;
    this.aa1 = aa1;
    this.aa2 = aa2;
    this.ab0 = ab0;
  }

  @Updater
  @Builder
  Bar<A, B> bar(Map<B, A> map0,
                Map<B, A> map1,
                Map<Map<B, A>, B> map2,
                Map<Map<B, A>, B> map3) {
    return new Bar<>(aa0, aa1, aa2, ab0, map0, map1, map2, map3);
  }

  static final class Bar<A, B extends A> {

    final A aa0;
    final A aa1;
    final A aa2;
    final B ab0;
    final Map<B, A> map0;
    final Map<B, A> map1;
    final Map<Map<B, A>, B> map2;
    final Map<Map<B, A>, B> map3;

    Bar(A aa0,
        A aa1,
        A aa2,
        B ab0,
        Map<B, A> map0,
        Map<B, A> map1,
        Map<Map<B, A>, B> map2,
        Map<Map<B, A>, B> map3) {
      this.aa0 = aa0;
      this.aa1 = aa1;
      this.aa2 = aa2;
      this.ab0 = ab0;
      this.map0 = map0;
      this.map1 = map1;
      this.map2 = map2;
      this.map3 = map3;
    }
  }
}
