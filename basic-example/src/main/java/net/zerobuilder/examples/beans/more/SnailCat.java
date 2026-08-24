package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Name;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.RecordUpdater;

import java.util.List;

@RecordBuilder
@RecordUpdater
record SnailCat<E>(
    E name,
    int age,
    List<E> notes,
    @Name("executive")
    boolean isExecutive) {
}
