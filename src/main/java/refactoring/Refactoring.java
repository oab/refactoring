package refactoring;

import org.abs_models.frontend.ast.*;

public abstract class Refactoring {
    protected Model model;
    public Refactoring(Model m) {
        this.model = m;
    }

    public void refactor(Match m) {
        m.refactor();
    }

    public abstract class Match {
        protected abstract void refactor();
    }

}
