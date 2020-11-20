package refactoring;

import org.abs_models.backend.common.InternalBackendException;
import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.abs_models.common.WrongProgramArgumentException;
import org.abs_models.frontend.ast.Model;
import org.abs_models.frontend.parser.Main;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.io.File;
import java.util.ArrayList;
import java.io.PrintWriter;

public class Refactor {

    // For now let's just hardcode some stuff to get going,
    // but probably should use the cmdline parsing library
    // already in use if this progresses beyond a one-off thing

    // args[0] Class (e.g Foo)
    // args[1] Method (e.g. bar)
    // args[2] call at line (e.g. 3)
    // args[3]... file(s)
    public static void main(String args[]) {

        // TODO: Proper argument handling?
        if(args.length != 3) {
            System.out.println("Expected: Class Method File");
            System.exit(1);
        }



        ArrayList<File> files  = new ArrayList<>();
        File in = new File(args[2]);

        // TODO: regexp match and rewrite *.abs -> *.after.abs
        File out = new File(args[2]+".after");
        files.add(in);

        Main entry = new Main();

        //TODO: Is the exception handling really needed?
        try {

            Model m = entry.parse(files);
            PrintWriter writer = new PrintWriter(out);
            ABSFormatter formatter = new DefaultABSFormatter(writer);

            hideDelegate(m,args[0],args[1]).doPrettyPrint(writer,formatter);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (WrongProgramArgumentException e) {
            e.printStackTrace();
        } catch (InternalBackendException e) {
            e.printStackTrace();
        }


    }

    // TODO: implement refactoring i.e. Model -> transformed Model
    // 1. Traverse to class
    // 2. Traverse to method in class
    public static Model hideDelegate(Model m, String classname, String methodname) {


        return m;
    }


}
