package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

@RecordBuilder
record Message(
        String sender,
        String body,
        String recipient
) {
}
