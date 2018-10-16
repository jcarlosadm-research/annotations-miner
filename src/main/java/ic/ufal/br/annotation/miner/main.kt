package ic.ufal.br.annotation.miner

import ic.ufal.br.annotation.miner.util.RepoM
import java.io.File
import org.repodriller.Study
import java.lang.Exception
import org.repodriller.scm.GitRemoteRepository
import org.repodriller.filter.range.Commits
import org.repodriller.RepoDriller
import ic.ufal.br.annotation.miner.visitor.MyCommitVisitor
import org.repodriller.persistence.csv.CSVFile
import java.util.Arrays
import org.repodriller.filter.commit.OnlyModificationsWithFileTypes
import org.repodriller.filter.commit.OnlyInBranches

const val OUTPUT_DIR = "output"
const val REPOS_DIR = "repos"

const val PROJECT_LIST_NAME = "projectList.txt"
const val REPORT_NAME = "report.csv"

fun main(args: Array<String>) {
    File(OUTPUT_DIR).mkdir()
    RepoDriller().start(MyStudy())
}

class MyStudy : Study {
    override fun execute() {
        try {
            RepoM().inLoc(GitRemoteRepository.hostedOn(File(PROJECT_LIST_NAME).readLines())
                            .inTempDir(REPOS_DIR).buildAsSCMRepositories())
                    .through(Commits.all())
                    .process(
                            MyCommitVisitor(),
                            CSVFile("$OUTPUT_DIR${File.separator}$REPORT_NAME")
                    )
                    .filters(
                            OnlyModificationsWithFileTypes(Arrays.asList(".java")),
                            OnlyInBranches(Arrays.asList("master"))
                    )
                    .mine()

        } catch (e: Exception) {
            println("error to mine")
        }
    }
}