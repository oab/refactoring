package refactoring;

import org.abs_models.backend.common.InternalBackendException;
import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.abs_models.common.WrongProgramArgumentException;
import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.parser.Main;
import org.abs_models.frontend.typechecker.KindedName;

import java.io.IOException;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.File;
import java.util.Iterator;

public class Refactor {

    // For now let's just hardcode some stuff to get going,
    // but probably should use the cmdline parsing library
    // already in use if this progresses beyond a one-off thing

    // args[0] Module
    // args[1] Class
    // args[2] Method
    // args[3] varname1
    // args[4] varname2
    // args[5]... file(s)
    public static void main(String args[]) {

        // TODO: Proper argument handling?
        if(args.length != 7) {
            System.out.println("Expected: Module Class Method AssignVar TargetVar MethodCall File");
            System.exit(1);
        }

        Main entry = new Main();

        //TODO: Is the exception handling really needed?
        try {

            Model m = entry.parse(Collections.singletonList(new File(args[6])));
            PrintWriter writer = new PrintWriter(new File(args[6]+".after"));
            ABSFormatter formatter = new DefaultABSFormatter(writer);

            hideDelegate(m,args[0],args[1],args[2],args[3],args[4],args[5])
                    .doPrettyPrint(writer,formatter);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (WrongProgramArgumentException e) {
            e.printStackTrace();
        } catch (InternalBackendException e) {
            e.printStackTrace();
        } catch (RefactoringException e) {
            e.printStackTrace();
        }


    }

    // TODO: implement refactoring i.e. Model -> transformed Model
    public static Model hideDelegate(Model m, String moduleName, String className, String methodName,
                                     String assignVar, String targetVar, String methodCall)
            throws RefactoringException {

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

        Iterator<Stmt> stmts = mImpl.getBlock().getStmts().iterator();

        // Match 1.
        /* Java 14
        while (stmts.hasNext()) {
            if(stmts.next() instanceof AssignStmt astmt1 &&
               astmt1.getVar().getName().equals(assignVar) &&
               astmt1.getValue() instanceof SyncCall syncallstmt1 &&
               syncallstmt1.getMethod().equals(methodCall) &&
               syncallstmt1.getCalleeNoTransform() instanceof VarUse v1 &&
               v1.getName().equals(targetVar) &&
               stmts.next() instanceof AssignStmt astmt2 &&
               astmt2.getValue() instanceof SyncCall syncallstmt2 &&
               syncallstmt2.getCalleeNoTransform() instanceof VarUse v2 &&
               v2.getName().equals(assignVar)) {
                return m;
            }
        }
        */

        Stmt match1;
        Stmt match2;
        Match: {
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

                break Match;

            }

            // Not found
            throw new RefactoringException(String.format("No match found"));
        }

        PrintWriter writer = new PrintWriter(System.out);
        ABSFormatter formatter = new DefaultABSFormatter(writer);
        match1.doPrettyPrint(writer,formatter);
        match2.doPrettyPrint(writer,formatter);

        return m;



    }


}
