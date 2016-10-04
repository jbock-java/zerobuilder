package net.zerobuilder.examples.beans;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

// inheritance
@Builders
@Goal(toBuilder = true)
public class Manager extends Employee {
}