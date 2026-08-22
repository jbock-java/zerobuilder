package net.zerobuilder.examples.beans.more;

import java.util.List;
import net.zerobuilder.Name;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.RecordUpdater;

@RecordBuilder
@RecordUpdater
record SnailCat<E>(
    E name,
    int age,
    List<String> notes,
    @Name("executive")
    boolean isExecutive) {
}
