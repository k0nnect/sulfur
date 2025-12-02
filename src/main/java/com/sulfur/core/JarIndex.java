package com.sulfur.core;
    
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
    
    public class JarIndex {
        private final Path jarPath;
        private final Map<String, String> classToEntry = new HashMap<>();
        private final Map<String, byte[]> classCache = new HashMap<>();
        private final Map<String, String> decompiledCodeCache = new HashMap<>();
    
        private JarIndex(Path jar) {
            this.jarPath = jar;
        }
    
        public static JarIndex fromJar(Path jar) throws IOException {
            JarIndex idx = new JarIndex(jar);
            try (JarFile jf = new JarFile(jar.toFile())) {
                Enumeration<JarEntry> en = jf.entries();
                while (en.hasMoreElements()) {
                    JarEntry e = en.nextElement();
                    if (e.isDirectory()) continue;
                    if (!e.getName().endsWith(".class")) continue;
                    String fqcn = e.getName()
                            .replace('/', '.')
                            .replaceAll("\\.class$", "");
                    idx.classToEntry.put(fqcn, e.getName());
                }
            }
            return idx;
        }
        
        public Set<String> classNames() {
            return Collections.unmodifiableSet(classToEntry.keySet());
        }
        
        public byte[] getClassBytes(String className) throws IOException {
            if (classCache.containsKey(className)) {
                return classCache.get(className);
            }

            String entryName = classToEntry.get(className);
            if (entryName == null) {
                throw new IOException("[!] Class not found in .jar file: " + className);
            }
            
            try (JarFile jf = new JarFile(jarPath.toFile())) {
                JarEntry entry = jf.getJarEntry(entryName);
                if (entry == null) {
                    throw new IOException("[!] .jar file entry not found: " + entryName);
                }
                
                byte[] bytes = readEntryBytes(jf, entry);
                classCache.put(className, bytes);
                return bytes;
            }
        }
        
        private byte[] readEntryBytes(JarFile jf, JarEntry entry) throws IOException {
            try (InputStream is = jf.getInputStream(entry)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        }
        
        public Path getJarPath() {
            return jarPath;
        }

        public void putDecompiledCode(String className, String decompiledCode) {
            this.decompiledCodeCache.put(className, decompiledCode);
        }

        public String getDecompiledCode(String className) {
            return this.decompiledCodeCache.get(className);
        }

        public InputStream openClass(String fqcn) throws IOException {
            String entry = classToEntry.get(fqcn);
            if (entry == null) throw new IOException("[!] Class not found: " + fqcn);
            JarFile jf = new JarFile(jarPath.toFile());
            return jf.getInputStream(jf.getJarEntry(entry));
        }
    }