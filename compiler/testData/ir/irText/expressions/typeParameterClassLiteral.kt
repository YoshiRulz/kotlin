// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

inline fun <reified T : Any> classRefFun() = T::class

inline fun <reified T : Any> Any.classRefExtFun() = T::class

inline val <reified T : Any> T.classRefExtVal
    get() = T::class


class Host {
    inline fun <reified TF : Any> classRefGenericMemberFun() = TF::class

    inline fun <reified TF : Any> Any.classRefGenericMemberExtFun() = TF::class

    inline val <reified TV : Any> TV.classRefGenericMemberExtVal
        get() = TV::class
}

