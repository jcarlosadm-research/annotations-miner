package ic.ufal.br.annotation.miner.report

import java.io.File

class ReportWriter(private val file: File) {

    fun writeLine(line: String) { file.bufferedWriter().use { out -> out.write("$line${System.lineSeparator()}") } }

    fun close() { file.bufferedWriter().close() }
}