package com.sulfur.core.deobfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for deobfuscating strings that have been obfuscated using Allatori.
 * Allatori typically uses special string loading methods and encrypted literals
 * @author k0nnect
 * @since 2025-04-10
 */
public class AllatoriDeobfuscator {

    // patterns for diff allatori string encryption styles
    
    // afaik allatori often uses a loading pattern like: String a = A.a("encrypted");
    private static final Pattern ALLATORI_PATTERN_1 = Pattern.compile(
            "(?:String\\s+)?(\\w+)\\s*=\\s*([A-Z][\\w]*)\\.(\\w+)\\(\"([^\"]+)\"\\s*(?:,[^)]+)?\\);");
    
    // another pattern uses character array loading which looks like this: loadString(new char[] { 65, 66, 67 })
    private static final Pattern ALLATORI_PATTERN_2 = Pattern.compile(
            "(?:String\\s+)?(\\w+)\\s*=\\s*(\\w+)\\(new\\s+char\\[\\]\\s*\\{([^}]+)\\}(?:,[^)]+)?\\);");
    
    // and some versions use string constants directly but encrypted
    private static final Pattern ALLATORI_STRING_CONSTANTS = Pattern.compile(
            "static\\s+(?:final\\s+)?String\\s+(\\w+)\\s*=\\s*\"([^\"]+)\";");
    
    // for finding potential string decryptor methods
    private static final Pattern DECRYPTOR_METHOD_PATTERN = Pattern.compile(
            "(?:private|protected|public|static|\\s)+String\\s+(\\w+)\\s*\\((?:String|char\\[\\])\\s+[^,)]+(?:,[^)]+)?\\)");

    private String decompiled;
    private Map<String, String> decryptedStrings;
    private List<String> potentialDecryptorMethods;
    private List<String> potentialDecryptorClasses;

    public AllatoriDeobfuscator() {
        this.decryptedStrings = new HashMap<>();
        this.potentialDecryptorMethods = new ArrayList<>();
        this.potentialDecryptorClasses = new ArrayList<>();
    }

    /**
     * process the decompiled code to identify and deobfuscate allatori string patterns.
     * 
     * @param decompiled the decompiled Java source code
     * @return processed source code with deobfuscated strings
     */
    public String process(String decompiled) {
        this.decompiled = decompiled;
        this.decryptedStrings.clear();
        this.potentialDecryptorMethods.clear();
        this.potentialDecryptorClasses.clear();
        
        // find potential decryptor methods and classes
        findPotentialDecryptors();
        processAllatoriPattern1();
        processAllatoriPattern2();
        processAllatoriStringConstants();
        
        // replace the encrypted strings with their deobfuscated values
        return replaceDeobfuscatedStrings();
    }
    
    private void findPotentialDecryptors() {
        Matcher methodMatcher = DECRYPTOR_METHOD_PATTERN.matcher(decompiled);
        while (methodMatcher.find()) {
            potentialDecryptorMethods.add(methodMatcher.group(1));
        }

        Pattern classPattern = Pattern.compile("class\\s+([A-Z][\\w]*)\\s+\\{");
        Matcher classMatcher = classPattern.matcher(decompiled);
        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            if (className.length() <= 2) {
                potentialDecryptorClasses.add(className);
            }
        }
    }
    
    private void processAllatoriPattern1() {
        Matcher matcher = ALLATORI_PATTERN_1.matcher(decompiled);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String className = matcher.group(2);
            String methodName = matcher.group(3);
            String encryptedValue = matcher.group(4);
            
            // check if this is likely an allatori decryptor class/method
            if (potentialDecryptorClasses.contains(className) || 
                methodName.length() <= 2 || 
                potentialDecryptorMethods.contains(methodName)) {
                
                String decryptedValue = simulateAllatoriDecryption(encryptedValue);
                decryptedStrings.put(varName, decryptedValue);
            }
        }
    }
    
    private void processAllatoriPattern2() {
        Matcher matcher = ALLATORI_PATTERN_2.matcher(decompiled);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String methodName = matcher.group(2);
            String charArrayValues = matcher.group(3);
            
            if (potentialDecryptorMethods.contains(methodName) || methodName.length() <= 2) {
                String decryptedValue = decryptCharArray(charArrayValues);
                if (decryptedValue != null) {
                    decryptedStrings.put(varName, decryptedValue);
                }
            }
        }
    }
    
    private void processAllatoriStringConstants() {
        Matcher matcher = ALLATORI_STRING_CONSTANTS.matcher(decompiled);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String encryptedValue = matcher.group(2);
            
            // check if this looks like an obfuscated string (contains many non-readable chars)
            if (isLikelyEncrypted(encryptedValue)) {
                String decryptedValue = simulateAllatoriDecryption(encryptedValue);
                decryptedStrings.put(varName, decryptedValue);
            }
        }
    }
    
    /**
     * determine if a string is likely encrypted (if it contains many non-alphanumeric chars)
     */
    private boolean isLikelyEncrypted(String str) {
        if (str.length() < 3) return false;
        
        int nonAlphaNumCount = 0;
        for (char c : str.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                nonAlphaNumCount++;
            }
        }
        
        // if more than 30% non-alphanumeric chars then its likely encrypted
        return (float)nonAlphaNumCount / str.length() > 0.3f;
    }
    
    /**
     * simulate allatori decryption for string encryption
     */
    private String simulateAllatoriDecryption(String encrypted) {
        try {
            // allatori often uses XOR, rotation, or other simple transformations
            
            StringBuilder decrypted = new StringBuilder();
            
            // try a common algorithm: xor w/ varying keys
            int key = 0x5A; // common starting key
            for (int i = 0; i < encrypted.length(); i++) {
                char c = encrypted.charAt(i);
                // apply xor and then rotate
                c ^= key;
                c = (char)((c << 3) | (c >> (16 - 3)));
                decrypted.append(c);
                
                // key evolution typical in allatori
                key = (key * 13 + c) & 0xFF;
            }
            
            // if result looks plausible, use it
            String result = decrypted.toString();
            if (isPrintable(result)) {
                return "/* [!] Allatori Deobfuscated: */ \"" + result + "\"";
            }
            
            // fallback to simple XOR w/ fixed key
            decrypted.setLength(0);
            for (char c : encrypted.toCharArray()) {
                decrypted.append((char)(c ^ 0x5A));
            }
            
            return "/* [!] Allatori Deobfuscated (simple): */ \"" + decrypted.toString() + "\"";
        } catch (Exception e) {
            return "/* [!] Allatori Deobfuscation failed */";
        }
    }
    
    /**
     * decrypt character array notation common in allatori
     */
    private String decryptCharArray(String charArrayValues) {
        try {
            StringBuilder sb = new StringBuilder();
            String[] values = charArrayValues.split("\\s*,\\s*");
            
            for (String value : values) {
                value = value.trim();
                if (value.isEmpty()) continue;
                
                // parse the character value
                int charValue;
                if (value.startsWith("0x") || value.startsWith("0X")) {
                    charValue = Integer.parseInt(value.substring(2), 16);
                } else {
                    charValue = Integer.parseInt(value);
                }
                
                // allatori often subtracts or XORs a value from each char
                // try to reverse common transformations
                char c = (char)charValue;
                sb.append(c);
            }
            
            return "/* [!] Allatori Deobfuscated: */ \"" + sb.toString() + "\"";
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * check if a string is mostly printable characters
     */
    private boolean isPrintable(String s) {
        int printableChars = 0;
        for (char c : s.toCharArray()) {
            if (c >= 32 && c < 127) { // ascii printable range
                printableChars++;
            }
        }
        
        // if more than 70% are printable, consider it valid
        return (float)printableChars / s.length() > 0.7f;
    }
    
    /**
     * replace encrypted strings with their deobfuscated values in the source code
     */
    private String replaceDeobfuscatedStrings() {
        StringBuilder processed = new StringBuilder(decompiled);
        
        // header
        if (!decryptedStrings.isEmpty()) {
            processed.insert(0, "/* [!] Sulfur - Allatori String Deobfuscation applied - found " 
                    + decryptedStrings.size() + " encrypted strings */\n\n");
        }

        for (Map.Entry<String, String> entry : decryptedStrings.entrySet()) {
            String varName = entry.getKey();
            String decryptedValue = entry.getValue();

            Pattern usagePattern = Pattern.compile("\\b" + varName + "\\b(?!\\s*=)");
            Matcher usageMatcher = usagePattern.matcher(processed);
            
            // StringBuilder has never been more useful :3
            StringBuilder result = new StringBuilder();
            int lastEnd = 0;
            
            while (usageMatcher.find()) {
                result.append(processed.substring(lastEnd, usageMatcher.start()));

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
