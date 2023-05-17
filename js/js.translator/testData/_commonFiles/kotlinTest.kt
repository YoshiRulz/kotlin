package kotlin.test
// the package is used for common with JVM tests

fun fail(message: String? = null): Nothing {
    throw Throwable(message)
}
