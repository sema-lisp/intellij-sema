package com.sema.intellij.notebook

import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.sema.intellij.config.SemaCommandLine
import java.net.HttpURLConnection
import java.net.URI
import java.net.ServerSocket

data class SemaNotebookSession(val url: String, val processHandler: ProcessHandler)

@Service(Service.Level.PROJECT)
class SemaNotebookSessionService(private val project: Project) : Disposable {
    private val sessions = mutableMapOf<String, SemaNotebookSession>()

    @Synchronized
    fun start(file: VirtualFile): SemaNotebookSession {
        sessions[file.path]?.let { existing ->
            if (!existing.processHandler.isProcessTerminated) return existing
            sessions.remove(file.path)
        }

        val port = allocatePort()
        val commandLine = SemaCommandLine.notebookServe(
            notebookPath = file.path,
            workingDirectory = file.parent?.path ?: project.basePath,
            port = port,
        )
        val handler = OSProcessHandler(commandLine)
        ProcessTerminatedListener.attach(handler)
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                synchronized(this@SemaNotebookSessionService) {
                    if (sessions[file.path]?.processHandler === handler) {
                        sessions.remove(file.path)
                    }
                }
            }
        })
        handler.startNotify()

        return SemaNotebookSession("http://127.0.0.1:$port", handler).also {
            sessions[file.path] = it
        }
    }

    @Synchronized
    fun stop(file: VirtualFile) {
        sessions.remove(file.path)?.processHandler?.destroyProcess()
    }

    fun waitUntilReady(session: SemaNotebookSession, timeoutMs: Long = 15_000): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() <= deadline) {
            if (session.processHandler.isProcessTerminated) {
                return "sema exited with code ${session.processHandler.exitCode} before serving the notebook"
            }

            try {
                val connection = URI.create("${session.url}/api/notebook").toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 500
                connection.readTimeout = 500
                connection.requestMethod = "GET"
                val status = connection.responseCode
                connection.disconnect()
                if (status < 500) return null
            } catch (_: Exception) {
                // The process may still be binding the loopback port.
            }

            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return "Interrupted while waiting for the Sema notebook server"
            }
        }
        return "Sema notebook server did not start in time"
    }

    @Synchronized
    override fun dispose() {
        sessions.values.forEach { it.processHandler.destroyProcess() }
        sessions.clear()
    }

    private fun allocatePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }

    companion object {
        fun getInstance(project: Project): SemaNotebookSessionService =
            project.getService(SemaNotebookSessionService::class.java)
    }
}
