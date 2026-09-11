package com.dinopig.piglauncher

import java.io.DataOutputStream

internal object RootShell {

    private const val TARGET_PACKAGE = "com.mi.android.globallauncher"

    fun restartPocoLauncher(): Boolean {
        return runCatching {
            val process = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()

            DataOutputStream(process.outputStream).use { output ->
                output.writeBytes("PIDS=\$(pidof $TARGET_PACKAGE)\n")
                output.writeBytes("if [ -z \"\$PIDS\" ]; then exit 1; fi\n")
                output.writeBytes("kill -9 \$PIDS\n")
                output.writeBytes("exit\n")
                output.flush()
            }

            process.inputStream.bufferedReader().use { it.readText() }

            process.waitFor() == 0
        }.getOrDefault(false)
    }
}
