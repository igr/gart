package dev.oblac.gart.hotreload

class ClassToReloadException(val className: String) : ClassNotFoundException(
    "Class $className needs hot reload, use new classloader instance!"
)
