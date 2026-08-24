package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;
import net.zerobuilder.RecordUpdater;

@RecordBuilder
@RecordUpdater
record Message(
        String sender,
        String body,
        String recipient
) {
}
