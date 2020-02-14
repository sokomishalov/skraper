package ru.sokomishalov.skraper.cli

/**
 * @author sokomishalov
 */
enum class OutputType(val extension: String) {
    LOG(".log"),
    CSV(".csv"),
    JSON(".json"),
    XML(".xml"),
    YAML(".yaml")
}