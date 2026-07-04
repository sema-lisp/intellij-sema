# Sema for IntelliJ

IntelliJ IDEA / JetBrains IDE support for [Sema](https://sema-lang.com), a Lisp dialect with first-class LLM primitives.

## Install

The plugin is pending publication on the [JetBrains Marketplace](https://plugins.jetbrains.com/). Once live, install it from your IDE via **Settings → Plugins → Marketplace** and search for **Sema**.

Until then (or to run a local build), build the plugin and install it from disk:

```bash
./gradlew buildPlugin
# Then in the IDE: Settings → Plugins → ⚙️ → Install Plugin from Disk…
# and pick build/distributions/Sema-<version>.zip
```

### Requirements

- IntelliJ IDEA 2024.3+ (or any JetBrains IDE on build 243+)
- [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) — installed automatically as a plugin dependency
- The `sema` binary on `PATH`, or point at it in **Settings → Languages & Frameworks → Sema**

## Features

- **Syntax highlighting** for `.sema`, `.semac` (bytecode), and `.sema-nb` (notebook) files, with a configurable color settings page
- **LSP integration** (via LSP4IJ): code completion, hover documentation, go-to-definition, references, rename, diagnostics, folding ranges, inlay hints, document highlight, semantic token coloring, call hierarchy, and clickable `import`/`load` path links
- **Code lenses** — evaluate top-level forms inline with `sema/evalResult` rendering, plus a "Clear Sema Results" action
- **Code formatting** — Reformat Code for Sema source (toggleable)
- **Structural editing** — brace matching, auto-pairing `()` `[]` `{}`, line (`;`) and block (`#| |#`) commenting, and Extend/Shrink Selection by s-expression
- **Debugging (DAP)** — step-through debugging with breakpoints, continue, step over/into/out, stack frames, scopes, and variable inspection (launches `sema dap`)
- **Sema Notebook editor** for `.sema-nb` files — live cell evaluation in a JCEF-backed view, run-all, open in an external browser, and export to Markdown
- **Run configurations** — right-click a `.sema` file to run it, or create a configuration from the Run menu
- **Custom file icons** for Sema source, compiled bytecode, and notebook files
- **Configurable binary location** with a Settings page and a missing-binary editor notification

## Development

This is a standard Gradle project using the [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html). Requires JDK 17+.

```bash
./gradlew buildPlugin   # build the distributable plugin ZIP -> build/distributions/
./gradlew test          # run unit tests
./gradlew runIde        # launch a sandbox IDE with the plugin installed
```

Full IDE integration tests (start a real IDE, install the plugin, verify startup) require a built ZIP:

```bash
./gradlew buildPlugin integrationTest
```

Signing and publishing to the Marketplace are covered in [RELEASING.md](RELEASING.md).

## Links

- [Sema language](https://sema-lang.com) · [Playground](https://sema.run) · [Source](https://github.com/HelgeSverre/sema)
- [This plugin's repository](https://github.com/sema-lisp/intellij-sema)
- [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij)
