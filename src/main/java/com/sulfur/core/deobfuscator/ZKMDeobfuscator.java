package com.sulfur.core.deobfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for deobfuscating strings that have been obfuscated using ZKM (Zelix KlassMaster).
 * ZKM typically uses a string decryption method and encrypted string literals.
 */
public class ZKMDeobfuscator {

    // patterns for different zkm string encryption styles
    private static final Pattern ZKM_PATTERN_1 = Pattern.compile(
            "(?:String\\s+)?(\\w+)\\s*=\\s*\\w+\\.(\\w+)\\(\"([^\"]+)\"(?:,\\s*(-?\\d+))?\\s*\\);");
    
    private static final Pattern ZKM_PATTERN_2 = Pattern.compile(
            "(?:static\\s+)?(?:final\\s+)?String\\s+(\\w+)\\s*=\\s*\\w+\\(\"([^\"]+)\"(?:,\\s*(-?\\d+))?\\s*\\);");
    
    // this is for finding potential string decryptor methods
    private static final Pattern DECRYPTOR_METHOD_PATTERN = Pattern.compile(
            "(?:private|protected|public|static|\\s)+String\\s+(\\w+)\\s*\\(String\\s+[^,]+(?:,\\s*int\\s+\\w+)?\\s*\\)\\s*\\{");
    
    // for finding string constants that may be encrypted
    private static final Pattern STRING_CONSTANTS_PATTERN = Pattern.compile(
            "static\\s+(?:final\\s+)?String\\s+(\\w+)\\s*=\\s*\"([^\"]+)\";");

    private String decompiled;
    private Map<String, String> decryptedStrings;
    private List<String> potentialDecryptorMethods;

    public ZKMDeobfuscator() {
        this.decryptedStrings = new HashMap<>();
        this.potentialDecryptorMethods = new ArrayList<>();
    }

    /**
     * Process the decompiled code to identify and deobfuscate ZKM string patterns.
     * @author k0nnect
     * @since 2025-04-10
     * @param decompiled The decompiled Java source code
     * @return Processed source code with deobfuscated strings
     */
    public String process(String decompiled) {
        this.decompiled = decompiled;
        this.decryptedStrings.clear();
        this.potentialDecryptorMethods.clear();
        
        // find potential decryptor methods for analysis
        findPotentialDecryptorMethods();
        
        // process diff ZKM patterns
        processZKMPattern1();
        processZKMPattern2();
        
        // replace the encrypted strings w/ their deobf values
        return replaceDeobfuscatedStrings();
    }
    
    private void findPotentialDecryptorMethods() {
        Matcher matcher = DECRYPTOR_METHOD_PATTERN.matcher(decompiled);
        while (matcher.find()) {
            potentialDecryptorMethods.add(matcher.group(1));
        }
    }
    
    private void processZKMPattern1() {
        Matcher matcher = ZKM_PATTERN_1.matcher(decompiled);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String methodName = matcher.group(2);
            String encryptedValue = matcher.group(3);
            String key = matcher.groupCount() >= 4 ? matcher.group(4) : null;
            
            // checks if this matches a known decryptor method
            if (potentialDecryptorMethods.contains(methodName)) {
                String decryptedValue = simulateZKMDecryption(encryptedValue, key);
                decryptedStrings.put(varName, decryptedValue);
            }
        }
    }
    
    private void processZKMPattern2() {
        Matcher matcher = ZKM_PATTERN_2.matcher(decompiled);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String encryptedValue = matcher.group(2);
            String key = matcher.groupCount() >= 3 ? matcher.group(3) : null;
            
            String decryptedValue = simulateZKMDecryption(encryptedValue, key);
            decryptedStrings.put(varName, decryptedValue);
        }
    }
    
    /**
     * Simulate ZKM decryption for known patterns
     */
    private String simulateZKMDecryption(String encrypted, String keyStr) {
        // - zkm decryption varies by version & cfg
        
        try {
            // a common ZKM encryption technique is XOR w/ a key
            int key = keyStr != null ? Integer.parseInt(keyStr) : 0xF;
            
            StringBuilder decrypted = new StringBuilder();
            for (int i = 0; i < encrypted.length(); i++) {
                char c = encrypted.charAt(i);
                // XOR and/or shifts
                c ^= key;
                decrypted.append(c);
            }
            
            return "/* [!] Deobfuscated: */ \"" + decrypted.toString() + "\"";
        } catch (Exception e) {
            return "/* [!] Deobfuscation failed */";
        }
    }
    
    /**
     * replace encrypted strings w/ their deobfuscated values in the src
     */
    private String replaceDeobfuscatedStrings() {
        StringBuilder processed = new StringBuilder(decompiled);
        
        // header
        if (!decryptedStrings.isEmpty()) {
            processed.insert(0, "/* [!] Sulfur - ZKM String Deobfuscation applied - found "
                    + decryptedStrings.size() + " encrypted strings */\n\n");
        }
        
        // [!] add comments w/ deobfuscated values
        for (Map.Entry<String, String> entry : decryptedStrings.entrySet()) {
            String varName = entry.getKey();
            String decryptedValue = entry.getValue();
            
            // [!] find usages of the variable and add a comment w/ the deobf value
            Pattern usagePattern = Pattern.compile("\\b" + varName + "\\b(?!\\s*=)");
            Matcher usageMatcher = usagePattern.matcher(processed);
            
            // use a StringBuilder to build the result with replacements
            StringBuilder result = new StringBuilder();
            int lastEnd = 0;
            
            while (usageMatcher.find()) {
                // append everything up to the match
                result.append(processed.substring(lastEnd, usageMatcher.start()));
                
                // append the match plus the comment
                result.append(varName).append(" /* ").append(decryptedValue).append(" */");
                
                lastEnd = usageMatcher.end();
            }
            
            // append the rest of the string
            result.append(processed.substring(lastEnd));
            
            // update processed with the new content
            processed = result;
        }
        
        return processed.toString();
    }
}
