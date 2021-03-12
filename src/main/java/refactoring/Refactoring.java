
/*
Problems with this idea

Every refactoring will very likely have a different match object.
With this it would be possible to have two refactorings
that produce two different matches and that you could mix up.

Having distinct types for every pair of refactorings and matches seem better
Can try to make them adhere to a common interface later.

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
*/