-dontobfuscate

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class com.android.inputmethod.latin.** { native <methods>; }
-keep class com.android.inputmethod.latin.makedict.** { *; }
-keep class com.android.inputmethod.latin.utils.BinaryDictionaryUtils { *; }
-keep class com.android.inputmethod.latin.DicTraverseSession { *; }
-keep class com.android.inputmethod.latin.BinaryDictionary { *; }
-keep class com.android.inputmethod.keyboard.ProximityInfo { *; }
