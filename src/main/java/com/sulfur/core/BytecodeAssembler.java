package com.sulfur.core;

import java.util.HashMap;
import java.util.Map;

public class BytecodeAssembler {

    private final JarIndex jarIndex;

    public BytecodeAssembler(JarIndex jarIndex) {
        this.jarIndex = jarIndex;
    }

    public byte[] assemble(String className, String disassembledText) throws Exception {
        return jarIndex.getClassBytes(className);
    }
}
