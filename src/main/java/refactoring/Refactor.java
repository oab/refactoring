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
    // args[3] call at line
    // args[4]... file(s)
    public static void main(String args[]) {

        // TODO: Proper argument handling?
        if(args.length != 5) {
            System.out.println("Expected: Module Class Method File");
            System.exit(1);
        }

        Main entry = new Main();

        //TODO: Is the exception handling really needed?
        try {

            Model m = entry.parse(Collections.singletonList(new File(args[4])));
            PrintWriter writer = new PrintWriter(new File(args[4]+".after"));
            ABSFormatter formatter = new DefaultABSFormatter(writer);

            hideDelegate(m,args[0],args[1],args[2],Integer.parseInt(args[3]))
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
    public static Model hideDelegate(Model m, String moduleName, String className, String methodName, int line)
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

        AssignStmt stmt1 = (AssignStmt) mImpl.getBlock().getStmt(line);
        if(stmt1 == null || !(stmt1 instanceof AssignStmt) ) {
            throw new RefactoringException(String.format("Line %i in method %s is not an assignment", line,methodName));
        }

        AssignStmt stmt2 = (AssignStmt) mImpl.getBlock().getStmt(line+1);
        if(stmt2 == null || !(stmt2 instanceof AssignStmt) ) {
            throw new RefactoringException(String.format("Line %i in method %s is not an assignment", line+1,methodName));
        }

        return m;


    }


}
