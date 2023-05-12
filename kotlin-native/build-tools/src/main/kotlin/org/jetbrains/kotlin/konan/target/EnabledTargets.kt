/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

private fun isTargetEnabled(target: KonanTarget) = when(target) {
    is KonanTarget.WATCHOS_X86,
    is KonanTarget.IOS_ARM32,
    is KonanTarget.MINGW_X86,
    is KonanTarget.LINUX_MIPS32,
    is KonanTarget.LINUX_MIPSEL32,
    is KonanTarget.WASM32,
    is KonanTarget.ZEPHYR -> false
    else -> true
}

/**
 * Targets for which to build.
 */
// TODO: Consider having a gradle property that allows overriding this set.
fun enabledTargets(platformManager: PlatformManager) = platformManager.enabled.filter(::isTargetEnabled)
