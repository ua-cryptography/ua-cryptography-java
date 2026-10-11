# The JNI library binds the native methods of NativeLibrary by name and calls its refusalOf back.
-keep class io.github.marketkernel.uacryptography.NativeLibrary {
    native <methods>;
    public static java.lang.Throwable refusalOf(int, byte[]);
}
