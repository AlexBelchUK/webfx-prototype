package dev.webfx.parse;

/**
 * Import type
 * 
 * Import string with a 'com.abc.*' is a WILDCARD type
 * and an import string with a 'com.abc.SomeClass' is a CLASS_NAME type
 */
public enum ImportType {
    WILDCARD, CLASS_NAME 
}
