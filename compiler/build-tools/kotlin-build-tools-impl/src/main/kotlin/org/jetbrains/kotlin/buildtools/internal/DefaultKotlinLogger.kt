/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

internal object DefaultKotlinLogger : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = TODO("Not yet implemented")

    override fun error(msg: String, throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun warn(msg: String) {
        TODO("Not yet implemented")
    }

    override fun info(msg: String) {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String) {
        TODO("Not yet implemented")
    }

    override fun lifecycle(msg: String) {
        TODO("Not yet implemented")
    }
}