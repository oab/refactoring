package refactoring;

import org.abs_models.backend.common.InternalBackendException;
import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.abs_models.common.WrongProgramArgumentException;
import org.abs_models.frontend.ast.Model;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

public class Main {

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

        org.abs_models.frontend.parser.Main entry = new org.abs_models.frontend.parser.Main();

        //TODO: Is the exception handling really needed?
        try {

            Model m = entry.parse(Collections.singletonList(new File(args[6])));
            PrintWriter writer = new PrintWriter(new File(args[6]+".after"));
            ABSFormatter formatter = new DefaultABSFormatter(writer);

            Refactor.hideDelegate(m,args[0],args[1],args[2],args[3],args[4],args[5])
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
}
