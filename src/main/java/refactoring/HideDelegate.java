package refactoring;

// this might be interesting
// import org.abs_models.frontend.parser.SourcePosition;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import java.util.Iterator;

public class HideDelegate extends Refactor {

    private final String inModule;
    private final String inClass;
    private final String inMethod;
    private final int line;

    public HideDelegate(String inModule, String inClass, String inMethod, int line) {
        this.inModule = inModule;
        this.inClass = inClass;
        this.inMethod = inMethod;
        this.line = line;
    }

    public void refactor(Model m) throws RefactoringException {

        ModuleDecl mDecl = m.lookupModule(inModule);
        if (mDecl == null) {
            throw new RefactoringException(String.format("Module by name %s not found", inModule));
        }

        ClassDecl cDecl = (ClassDecl)mDecl.lookup(new KindedName(KindedName.Kind.CLASS, inClass));
        if(cDecl == null) {
            throw new RefactoringException(String.format("Class by name %s not found", inClass));
        }

        MethodImpl mImpl = cDecl.lookupMethod(inMethod);
        if(mImpl == null) {
            throw new RefactoringException(String.format("Method by name %s not found", inMethod));
        }

        // Matching against e.g
        // 1. d = p.getDept();
        // 2. m = d.getManager();
        // Assume p also implements getManager (TODO assume -> ensure)
        // remove 1. and 2. and replace with 3.
        // 3. m = p.getManager();

	// TODO: Split finding instances/first instance from actual refactoring?
        Match candidate = find(mImpl.getBlock().getStmtList());

        // We can refactor
        if (candidate instanceof Pattern1Match) {
            Pattern1Match pattern1 = (Pattern1Match) candidate;

            // Construct the replacement node
            SyncCall call = new SyncCall()
                    .setMethod(pattern1.endMethod)
                    .setCallee(new VarUse().setName(pattern1.callVar1));

            AssignStmt replacement = new AssignStmt()
                    .setVar(new VarUse().setName(pattern1.assignVar2))
                    .setValue(call);

            pattern1.match.replaceWith(replacement);
            pattern1.block.removeChild(pattern1.block.getIndexOfChild(pattern1.next));

        }
        // We could not refactor
        else {
          String message = "";
          if(candidate instanceof NoMatch) {
              NoMatch nomatch = (NoMatch) candidate;
              message = nomatch.error;

          }
          throw new RefactoringException(message);
        }

    }

    private  Match find(List<Stmt> stmtlist) {
        Iterator<Stmt> stmts = stmtlist.iterator();

        //find the line match should be at
        while(stmts.hasNext()) {
            Stmt match = stmts.next();
            int atLine = match.getStartLine();

            if(atLine < line) {
                if (match instanceof Block) {
                    Block b = (Block) match;
                    Match descend = find(b.getStmtList());
                    if (!(descend instanceof NoMatch)) {
                        return descend;
                    }
                }
            } else if(atLine == line) {
                if(stmts.hasNext()) {
                    Stmt next = stmts.next();
                    return tryPattern1Match(match,next,stmtlist);
                }
            } else break;
        }
        return new NoMatch("No line matched");
    }

    private static abstract class Match {}
    private static class NoMatch extends Match {
        String error = "";
        private NoMatch(){}
        private NoMatch(String error){
            this.error = error;
        }
    }

    // Terminology used here:
    // assignVar1 = callVar1.middleMethod();
    // assignVar2 = callVar2.endMethod();
    //
    // The refactoring (at this spot only) is
    // assignVar2 = callvar1.endMethod();
    private static class Pattern1Match extends Match {
        AssignStmt match;
        AssignStmt next;
        List<Stmt> block;
        String assignVar1;
        String assignVar2;
        String callVar1;
        String callVar2;
        String middleMethod;
        String endMethod;
        private Pattern1Match(AssignStmt match, AssignStmt next,List<Stmt> block,
                              String assignVar1, String assignVar2, String callVar1,
                              String callVar2, String middleMethod, String endMethod) {
            this.match = match;
            this.next = next;
            this.block = block;
            this.assignVar1 = assignVar1;
            this.assignVar2 = assignVar2;
            this.callVar1 = callVar1;
            this.callVar2 = callVar2;
            this.middleMethod = middleMethod;
            this.endMethod = endMethod;

        }
    }

    private static Match tryPattern1Match(Stmt match, Stmt next,List<Stmt> block) {
        AssignStmt tryMatch;
        AssignStmt tryNext;
        String tryAssignVar1;
        String tryAssignVar2;
        String tryCallVar1;
        String tryCallVar2;
        String tryMiddleMethod;
        String tryEndMethod;

        if (!(match instanceof AssignStmt))
            return new NoMatch("No assignment at line");

        tryMatch = (AssignStmt) match;

        tryAssignVar1 = tryMatch.getVar().getName();

        if (!(tryMatch.getValue() instanceof SyncCall))
            return new NoMatch("No method call at line");

        SyncCall syncallstmt1 = (SyncCall) tryMatch.getValue();

        tryMiddleMethod = syncallstmt1.getMethod();

        if (!(syncallstmt1.getCalleeNoTransform() instanceof VarUse))
            return new NoMatch("No method call on variable at line");

        tryCallVar1 = ((VarUse) syncallstmt1.getCalleeNoTransform()).getName();

        if (!(next instanceof AssignStmt))
            return new NoMatch();

        tryNext = (AssignStmt) next;
        tryAssignVar2 = tryNext.getVar().getName();

        if (!(tryNext.getValue() instanceof SyncCall))
            return new NoMatch("No method call at next line");

        SyncCall syncallstmt2 = (SyncCall) tryNext.getValue();

        if (!(syncallstmt2.getCalleeNoTransform() instanceof VarUse))
            return new NoMatch("No method call on variable at next line");

        tryCallVar2 = ((VarUse) syncallstmt2.getCalleeNoTransform()).getName();

        tryEndMethod = syncallstmt2.getMethod();

        if (!tryAssignVar1.equals(tryCallVar2))
            return new NoMatch("Variable being assigned to at line not equal " +
                                     "to method call variable in next line");

        return new Pattern1Match(tryMatch,tryNext,block,tryAssignVar1,tryAssignVar2,
                                 tryCallVar1,tryCallVar2,tryMiddleMethod,tryEndMethod);
    }

}
