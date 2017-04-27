package com.ljc;

import java.util.ArrayList;

@FunctionalInterface
public interface JispCompiledFunction {
    Object evaluate(ArrayList items, JispInterp context);
}
