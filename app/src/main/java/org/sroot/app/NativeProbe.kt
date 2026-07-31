package org.sroot.app

object NativeProbe {
    init {
        System.loadLibrary("sroot_native")
    }

    external fun run(): String
}
