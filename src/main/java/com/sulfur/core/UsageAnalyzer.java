package com.sulfur.core;

import com.sulfur.core.JarIndex;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsageAnalyzer {

    private final JarIndex jarIndex;

    public UsageAnalyzer(JarIndex jarIndex) {
        this.jarIndex = jarIndex;
    }

    public Set<String> findUsages(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> usages = new HashSet<>();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(searchTerm) + "\\b");

        for (String className : jarIndex.classNames()) {
            String decompiledCode = jarIndex.getDecompiledCode(className);
            if (decompiledCode != null) {
                Matcher matcher = pattern.matcher(decompiledCode);
                if (matcher.find()) {
                    usages.add(className);
                }
            }
        }
        return usages;
    }
}
