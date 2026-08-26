package net.zerobuilder.examples.beans.more;

import net.zerobuilder.StepName;
import net.zerobuilder.RecordBuilder;

import java.util.List;

@RecordBuilder
record SnailCat<E>(
    E name,
    int age,
    List<E> notes,
    @StepName("executive")
    boolean isExecutive) {
}
