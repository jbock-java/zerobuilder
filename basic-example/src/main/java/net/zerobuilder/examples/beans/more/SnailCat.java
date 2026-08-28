package net.zerobuilder.examples.beans.more;

import net.zerobuilder.RecordBuilder;
import net.zerobuilder.StepName;

import java.util.List;

@RecordBuilder
record SnailCat<E>(
    E name,
    int age,
    List<E> notes,
    @StepName("executive")
    boolean isExecutive) {
}
