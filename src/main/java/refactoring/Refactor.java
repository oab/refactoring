package refactoring;

import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import java.io.PrintWriter;
import java.util.Iterator;

public class Refactor {

    // TODO: implement refactoring i.e. Model -> transformed Model
    public static Model hideDelegate(Model m,
                                     String moduleName,
                                     String className,
                                     String methodName,
                                     String assignVar,
                                     String targetVar,
                                     String methodCall ) throws RefactoringException {

        ModuleDecl mDecl = m.lookupModule(moduleName);
        if (mDecl == null) {
            throw new RefactoringException(String.format("Module by name %s not found", moduleName));
        }

        ClassDecl cDecl = (ClassDecl)mDecl.lookup(new KindedName(KindedName.Kind.CLASS,className));
        if(cDecl == null) {
            throw new RefactoringException(String.format("Class by name %s not found", className));
        }

        MethodImpl mImpl = cDecl.lookupMethod(methodName);
        if(mImpl == null) {
            throw new RefactoringException(String.format("Method by name %s not found", methodName));
        }

        // Matching against e.g
        // 1. d = p.getDept();
        // 2. m = d.getManager();
        // Assume p also implements getManager (TODO assume -> ensure)
        // remove 1. and 2. and replace with 3.
        // 3. m = p.getManager();

        //Iterator<Stmt> stmts = mImpl.getBlock().getStmts().iterator();
        List<Stmt> stmtlist = mImpl.getBlock().getStmtList();

        MatchAndReplace: {
            Iterator<Stmt> stmts = stmtlist.iterator();
            Stmt match1;
            Stmt match2;


            while (stmts.hasNext()) {
                match1 = stmts.next();
                if (!(match1 instanceof AssignStmt)) continue;
                AssignStmt astmt1 = (AssignStmt) match1;

                if (!astmt1.getVar().getName().equals(assignVar)) continue;

                if(!(astmt1.getValue() instanceof SyncCall)) continue;
                SyncCall syncallstmt1 = (SyncCall) astmt1.getValue();

                if(!(syncallstmt1.getMethod().equals(methodCall))) continue;

                if(!(syncallstmt1.getCalleeNoTransform() instanceof VarUse)) continue;
                VarUse v1 = (VarUse) syncallstmt1.getCalleeNoTransform();

                if(!(v1.getName().equals(targetVar))) continue;

                match2 = stmts.next();
                if(!(match2 instanceof AssignStmt)) continue;
                AssignStmt astmt2 = (AssignStmt) match2;

                if(!(astmt2.getValue() instanceof SyncCall)) continue;
                SyncCall syncallstmt2 = (SyncCall) astmt2.getValue();

                if(!(syncallstmt2.getCalleeNoTransform() instanceof VarUse)) continue;
                VarUse v2 =  (VarUse) syncallstmt2.getCalleeNoTransform();

                if(!v2.getName().equals(assignVar)) continue;

                // Construct the replacement node
                SyncCall call = new SyncCall()
                        .setMethod(syncallstmt2.getMethod())
                        .setCallee(new VarUse().setName(targetVar));

                AssignStmt replacement = new AssignStmt()
                        .setVar(new VarUse().setName(astmt2.getVar().getName()))
                        .setValue(call);

                match1.replaceWith(replacement);

                // Remove the line after the replacement
                stmtlist.removeChild(stmtlist.getIndexOfChild(match2));


                break MatchAndReplace;

            }

            // Not found
            throw new RefactoringException(String.format("No match found"));
        }

        return m;



    }


}
