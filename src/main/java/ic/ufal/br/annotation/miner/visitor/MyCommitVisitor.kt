package ic.ufal.br.annotation.miner.visitor

import ic.ufal.br.annotation.miner.OUTPUT_DIR

import org.repodriller.scm.CommitVisitor
import org.repodriller.scm.SCMRepository
import org.repodriller.domain.Commit
import org.repodriller.persistence.PersistenceMechanism
import java.util.regex.Pattern
import java.io.File
import org.repodriller.domain.DiffParser
import java.lang.Exception
import org.repodriller.domain.Modification
import ic.ufal.br.annotation.miner.report.ReportWriter
import org.repodriller.domain.DiffBlock

class MyCommitVisitor : CommitVisitor {
    private val patternOk = Pattern.compile("\\s@[a-zA-Z]+")!!
    private val patternNotOk = Pattern.compile("(//|\\*)[^@]*@[a-zA-Z]+")!!

    private val separatorBar = "===================================${System.lineSeparator()}"

    private val prohibitedAnnotationFilePath = "prohibited_annotations.txt"

    override fun process(repo: SCMRepository, commit: Commit, writer: PersistenceMechanism?) {
        val folder = File("$OUTPUT_DIR${File.separator}${getProjectName(repo)}")
        folder.mkdir()

        for (modification in commit.modifications) {
            if (!modification.fileNameEndsWith(".java")) continue

            var detectedLines = false
            val diffParser = DiffParser(modification.diff)

            try {
                val reportWriter = ReportWriter(File(buildReportPath(folder, commit, modification)))

                for (diffBlock in diffParser.blocks) {
                    detectedLines = checkBlock(diffBlock, detectedLines, folder, commit, modification, repo, reportWriter)
                }

                writeCommitMsgAndDiffBlock(detectedLines, reportWriter, commit, diffParser)

                reportWriter.close()
            } catch (e: Exception) {
                println("error to write report")
            }
        }
    }

    private fun writeCommitMsgAndDiffBlock(detectedLines: Boolean, reportWriter: ReportWriter, commit: Commit,
                                           diffParser: DiffParser) {
        if (detectedLines) {
            reportWriter.writeLine(separatorBar)
            reportWriter.writeLine("Commit message:${System.lineSeparator()}")
            reportWriter.writeLine(commit.msg)

            reportWriter.writeLine(separatorBar)
            reportWriter.writeLine("Diff Block:${System.lineSeparator()}")
            reportWriter.writeLine(diffParser.fullDiff)
        }
    }

    private fun checkBlock(diffBlock: DiffBlock, detectedLines: Boolean, folder: File, commit: Commit,
                           modification: Modification, repo: SCMRepository, reportWriter: ReportWriter): Boolean {
        var detectL = detectedLines
        for(line in diffBlock.lines) {
            if(lineOk(line) && hasAnnotation(line)) {
                if(!detectedLines) {
                    writeHeadToReport(folder, commit, modification, repo, reportWriter)
                    detectL = true
                }
                reportWriter.writeLine(line)
            }
        }

        return detectL
    }

    private fun writeHeadToReport(folder: File, commit: Commit, modification: Modification, repo: SCMRepository,
                                  reportWriter: ReportWriter) {
        File(folder.absolutePath + File.separator + commit.hash).mkdirs()
        val headString = "Basic Info:${System.lineSeparator()}" + System.lineSeparator() +
                "Commit: ${commit.hash + System.lineSeparator()}" +
                "File: ${modification.fileName + System.lineSeparator()}" +
                "URL: ${buildCommitURL(repo, commit) + System.lineSeparator()}" +
                separatorBar +
                System.lineSeparator() +
                "Detected Lines: ${System.lineSeparator()}" + System.lineSeparator()
        reportWriter.writeLine(headString)
    }

    private fun buildCommitURL(repo: SCMRepository, commit: Commit): String {
        var uri = repo.origin
        if (uri.endsWith(".git"))
            uri = uri.substring(0, uri.lastIndexOf(".git"))
        return uri + "/commit/" + commit.hash
    }

    private fun hasAnnotation(line: String): Boolean = if (containsProhibitedWords(line)) { false } else
        !patternNotOk.matcher(line).find() && patternOk.matcher(line).find()

    private fun containsProhibitedWords(line: String): Boolean {
        try {
            val prohibitedWords = File(prohibitedAnnotationFilePath).readLines()
            prohibitedWords.forEach { word -> if(line.contains(word)) return true }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun lineOk(line: String): Boolean = !(line.startsWith("@@ ") || (!line.startsWith("+") &&
            !line.startsWith("-")))

    private fun buildReportPath(baseFolder: File, commit: Commit, modification: Modification): String {
        return baseFolder.absolutePath + File.separator + commit.hash + File.separator +
                modification.fileName.substring(0, modification.fileName.lastIndexOf("."))
    }

    private fun getProjectName(repo: SCMRepository): String {
        var uri = repo.origin
        if (uri.endsWith("/")) uri = uri.substring(0, uri.length - 1)
        return uri.substring(uri.lastIndexOf("/") + 1)
    }
}