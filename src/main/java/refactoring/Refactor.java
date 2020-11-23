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
        if(args.length != 4) {
            System.out.println("Expected: Module Class Method File");
            System.exit(1);
        }

        Main entry = new Main();

        //TODO: Is the exception handling really needed?
        try {

            Model m = entry.parse(Collections.singletonList(new File(args[3])));
            PrintWriter writer = new PrintWriter(new File(args[3]+".after"));
            ABSFormatter formatter = new DefaultABSFormatter(writer);

            hideDelegate(m,args[0],args[1],args[2]).doPrettyPrint(writer,formatter);

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
    // 1. Traverse to class
    // 2. Traverse to method in class
    public static Model hideDelegate(Model m, String moduleName, String className, String methodName)
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

        List<Stmt> stmts =  mImpl.getBlock().getStmtList();


        return m;


    }


}
