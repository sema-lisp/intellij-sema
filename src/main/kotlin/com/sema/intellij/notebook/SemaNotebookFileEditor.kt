package com.sema.intellij.notebook

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.sema.intellij.config.SemaBinary
import com.sema.intellij.config.SemaConfigurable
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class SemaNotebookFileEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {
    private var browser: JBCefBrowser? = null
    private var disposed = false
    private val component: JComponent = createComponent()

    private fun createComponent(): JComponent {
        val status = SemaBinary.currentStatus()
        if (!status.available) {
            return JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(16)
                add(JBLabel(status.errorText ?: SemaBinary.missingBinaryMessage()), BorderLayout.NORTH)
                add(
                    JButton("Configure").apply {
                        addActionListener {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, SemaConfigurable::class.java)
                        }
                    },
                    BorderLayout.SOUTH,
                )
            }
        }

        val session = SemaNotebookSessionService.getInstance(project).start(file)
        if (JBCefApp.isSupported()) {
            val panel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(16)
                add(JBLabel("Starting Sema notebook…"), BorderLayout.NORTH)
            }
            val cefBrowser = JBCefBrowser().also { browser = it }
            whenNotebookReady(session) { error ->
                panel.removeAll()
                if (error == null) {
                    cefBrowser.loadURL(session.url)
                    panel.border = null
                    panel.add(cefBrowser.component, BorderLayout.CENTER)
                } else {
                    panel.add(JBLabel("Could not open the Sema notebook: $error"), BorderLayout.NORTH)
                }
                panel.revalidate()
                panel.repaint()
            }
            return panel
        }

        val openButton = JButton("Open in Browser").apply {
            isEnabled = false
            addActionListener { BrowserUtil.browse(session.url, project) }
        }
        return JPanel(BorderLayout()).also { panel ->
            val statusLabel = JBLabel("Starting Sema notebook…")
            panel.apply {
                border = JBUI.Borders.empty(16)
                add(statusLabel, BorderLayout.NORTH)
                add(openButton, BorderLayout.SOUTH)
            }
            whenNotebookReady(session) { error ->
                statusLabel.text = error ?: "JCEF is not available in this IDE runtime."
                openButton.isEnabled = error == null
            }
        }
    }

    private fun whenNotebookReady(session: SemaNotebookSession, update: (String?) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val error = SemaNotebookSessionService.getInstance(project).waitUntilReady(session)
            SwingUtilities.invokeLater {
                if (!disposed && !project.isDisposed && file.isValid) update(error)
            }
        }
    }

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = component

    override fun getName(): String = "Notebook"

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getFile(): VirtualFile = file

    override fun getState(level: FileEditorStateLevel): FileEditorState =
        FileEditorState.INSTANCE

    override fun dispose() {
        disposed = true
        browser?.dispose()
        SemaNotebookSessionService.getInstance(project).stop(file)
    }
}
