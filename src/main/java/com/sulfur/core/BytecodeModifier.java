package com.sulfur.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

public class BytecodeModifier {

    public static byte[] addNoOpMethod(byte[] originalBytes, String className) {
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visitEnd() {
                MethodVisitor mv = cv.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "newMethod",
                        "()V",
                        null, null);
                mv.visitCode();
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                super.visitEnd();
            }
        }, 0);
        return cw.toByteArray();
    }

    public static byte[] addField(byte[] originalBytes, String className, String fieldName, String descriptor, int access) {
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visitEnd() {
                FieldVisitor fv = cv.visitField(access, fieldName, descriptor, null, null);
                fv.visitEnd();
                super.visitEnd();
            }
        }, 0);
        return cw.toByteArray();
    }

    public static byte[] changeMethodAccess(byte[] originalBytes, String className, String methodName, String methodDescriptor, int newAccess) {
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals(methodName) && descriptor.equals(methodDescriptor)) {
                    return super.visitMethod(newAccess, name, descriptor, signature, exceptions);
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return cw.toByteArray();
    }

    public static byte[] replaceStringLiteral(byte[] originalBytes, String className, String targetMethodName, String targetMethodDescriptor, String oldString, String newString) {
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals(targetMethodName) && descriptor.equals(targetMethodDescriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof String && value.equals(oldString)) {
                                super.visitLdcInsn(newString);
                            } else {
                                super.visitLdcInsn(value);
                            }
                        }
                    };
                }
                return mv;
            }
        }, 0);
        return cw.toByteArray();
    }
}
