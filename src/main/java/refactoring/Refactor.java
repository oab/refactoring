package refactoring;

import org.abs_models.backend.common.InternalBackendException;
import org.abs_models.common.WrongProgramArgumentException;
import org.abs_models.frontend.ast.Model;
import org.abs_models.frontend.parser.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.util.ArrayList;

public class Refactor {

    // For now let's just hardcode some stuff to get going,
    // but probably should use the cmdline parsing library
    // already in use if this progresses beyond a one-off thing

    // args[0] Class (e.g Foo)
    // args[1] Method (e.g. bar)
    // args[2] call at line (e.g. 3)
    // args[3]... file(s)
    public static void main(String args[]) {
        File in = new File(args[0]);
        ArrayList<File> files  = new ArrayList<>();
        files.add(in);

        Main entry = new Main();
        try {
            Model m = entry.parse(files);
            m.lookupModule("HideDelegate").getDecl(3).dump(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WrongProgramArgumentException e) {
            e.printStackTrace();
        } catch (InternalBackendException e) {
            e.printStackTrace();
        }


    }

    // implement refactoring i.e. Model -> transformed Model -> prettyprint & emit as file
    public static Model hideDelegate(Model m) {

        return m;
    }


}
