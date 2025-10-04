package com.sulfur.core;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DecompilerService {
    
    public static String decompile(JarIndex index, String className) throws Exception {
        byte[] classBytes = index.getClassBytes(className);
        return decompileWithProcyon(classBytes, className);
    }
    
    public static String decompileWithCFR(JarIndex index, String className) throws Exception {
        byte[] classBytes = index.getClassBytes(className);

        Map<String, String> options = new HashMap<>();
        options.put("showversion", "false");
        options.put("hidebridgemethods", "true");
        options.put("hidelongstrings", "true");
        options.put("decodestringswitch", "true");
        options.put("sugarenums", "true");
        options.put("decodelambdas", "true");
        options.put("comments", "true");

        final StringBuilder result = new StringBuilder();
        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                return sinkType == SinkType.JAVA ? str -> result.append(str) : ignored -> {};
            }
        };

        CfrDriver driver = new CfrDriver.Builder()
                .withOutputSink(mySink)
                .withOptions(options)
                .build();

        driver.analyse(Collections.singletonList(className + ".class"));
        
        return "/* Decompiled with CFR x Sulfur */\n" + result.toString();
    }
    
    private static String decompileWithProcyon(byte[] classBytes, String className) {
        try {
            DecompilerSettings settings = DecompilerSettings.javaDefaults();
            settings.setForceExplicitImports(true);
            settings.setForceExplicitTypeArguments(true);
            settings.setShowSyntheticMembers(false);
            settings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());

            ITypeLoader typeLoader = new InputTypeLoader() {
                @Override
                public boolean tryLoadType(String internalName, Buffer buffer) {
                    if (internalName.equals(className.replace('.', '/'))) {
                        buffer.reset(classBytes.length);
                        buffer.putByteArray(classBytes, 0, classBytes.length);
                        buffer.position(0);
                        return true;
                    }
                    return false;
                }
            };

            MetadataSystem metadataSystem = new MetadataSystem(typeLoader);

            String internalName = className.replace('.', '/');
            TypeReference type = metadataSystem.lookupType(internalName);
            TypeDefinition resolvedType = null;
            
            if (type != null) {
                resolvedType = type.resolve();
            }
            
            if (resolvedType == null) {
                return "/* Error: Could not resolve type */";
            }

            DecompilationOptions options = new DecompilationOptions();
            options.setSettings(settings);

            StringWriter stringWriter = new StringWriter();
            PlainTextOutput output = new PlainTextOutput(stringWriter);
            settings.getLanguage().decompileType(resolvedType, output, options);
            
            return "/* Decompiled with Procyon x Sulfur */\n" + stringWriter.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "/* Decompilation error:\n" + sw.toString() + "\n*/";
        }
    }
}