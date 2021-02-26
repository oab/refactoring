package refactoring;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;

public abstract class Refactor {
    public abstract void refactor(Model m) throws RefactoringException;
}
