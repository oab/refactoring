package refactoring;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import java.util.Iterator;

public class HideDelegate extends Refactor {

    private final String inModule;
    private final String inClass;
    private final String inMethod;
    private final String assignVar;
    private final String targetVar;
    private final String middleMethod;
    private final String endMethod;

    public HideDelegate(String inModule,
                        String inClass,
                        String inMethod,
                        String assignVar,
                        String targetVar,
                        String middleMethod,
                        String endMethod){
        this.inModule = inModule;
        this.inClass = inClass;
        this.inMethod = inMethod;
        this.assignVar = assignVar;
        this.targetVar = targetVar;
        this.middleMethod = middleMethod;
        this.endMethod = endMethod;
    }

    public Model refactor(Model m) throws RefactoringException {

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

        // Construct the replacement node
        SyncCall call = new SyncCall()
                .setMethod(endMethod)
                .setCallee(new VarUse().setName(targetVar));

        AssignStmt replacement = new AssignStmt()
                .setVar(new VarUse().setName(assignVar))
                .setValue(call);

        List<Stmt> stmtlist = mImpl.getBlock().getStmtList();


        if(replace(replacement,stmtlist)) return m;

        // Not found
        throw new RefactoringException("No match found");



    }

    private boolean replace(AssignStmt replacement, List<Stmt> stmtlist) {
        Iterator<Stmt> stmts = stmtlist.iterator();
        Stmt match1;
        Stmt match2;

        while (stmts.hasNext()) {
            match1 = stmts.next();

            if (match1 instanceof Block) {
                Block b = (Block) match1;
                if(replace(replacement,b.getStmtList())) return true;
            }

            if (!(match1 instanceof AssignStmt)) continue;
            AssignStmt astmt1 = (AssignStmt) match1;
            if (!astmt1.getVar().getName().equals(assignVar)) continue;
            if (!(astmt1.getValue() instanceof SyncCall)) continue;
            SyncCall syncallstmt1 = (SyncCall) astmt1.getValue();
            if (!(syncallstmt1.getMethod().equals(middleMethod))) continue;
            if (!(syncallstmt1.getCalleeNoTransform() instanceof VarUse)) continue;
            VarUse v1 = (VarUse) syncallstmt1.getCalleeNoTransform();
            if (!(v1.getName().equals(targetVar))) continue;

            match2 = stmts.next();
            if (!(match2 instanceof AssignStmt)) continue;
            AssignStmt astmt2 = (AssignStmt) match2;
            if (!(astmt2.getValue() instanceof SyncCall)) continue;
            SyncCall syncallstmt2 = (SyncCall) astmt2.getValue();
            if (!(syncallstmt2.getCalleeNoTransform() instanceof VarUse)) continue;
            VarUse v2 = (VarUse) syncallstmt2.getCalleeNoTransform();
            if (!v2.getName().equals(assignVar)) continue;

            match1.replaceWith(replacement);

            // Remove the line after the replacement
            stmtlist.removeChild(stmtlist.getIndexOfChild(match2));

            return true;
        }
        return false;
    }





}
