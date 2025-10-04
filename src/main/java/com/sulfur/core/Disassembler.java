package com.sulfur.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Disassembler {
    public static String disassemble(JarIndex index, String fqcn) throws IOException {
        try (InputStream in = index.openClass(fqcn)) {
            ClassReader cr = new ClassReader(in);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            TraceClassVisitor tcv = new TraceClassVisitor(null, new Textifier(), pw);
            cr.accept(tcv, 0);
            pw.flush();
            return sw.toString();
        }
    }
}