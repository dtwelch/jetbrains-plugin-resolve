package edu.clemson.resolve.plugin.psi;

import org.jetbrains.annotations.Nullable;

public interface ResMathNamedElement extends ResNamedElement, ResMathTypeOwner {

    @Nullable ResMathType findSiblingType();
}