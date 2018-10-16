package ic.ufal.br.annotation.miner.util;

import org.repodriller.RepositoryMining;
import org.repodriller.scm.SCMRepository;

public class RepoM extends RepositoryMining {
	public RepositoryMining inLoc(SCMRepository[] repo) {
		return this.in(repo);
	}
}
