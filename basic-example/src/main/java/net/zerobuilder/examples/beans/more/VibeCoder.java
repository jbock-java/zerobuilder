package net.zerobuilder.examples.beans.more;

import java.util.List;
import net.zerobuilder.Name;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.RecordUpdater;

@RecordBuilder
@RecordUpdater
public record VibeCoder(
    String name,
    int age,
    List<String> notes,
    @Name("executive")
    boolean isExecutive) {

}
