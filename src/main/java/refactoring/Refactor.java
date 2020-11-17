package refactoring;

import org.abs_models.backend.common.InternalBackendException;
import org.abs_models.common.WrongProgramArgumentException;
import org.abs_models.frontend.ast.Model;
import org.abs_models.frontend.parser.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Refactor {

    // For now let's just hardcode some stuff to get going
    // Probably should use the cmdline parsing library

    // args[0] file
    // args[1] Class (e.g Foo)
    // args[2] Method (e.g. bar)
    // args[4] call at line (e.g. 3)
    public static void  main(String args[]) {
        File in = new File(args[0]);
        ArrayList<File> files  = new ArrayList<>();
        files.add(in);

        Main entry = new Main();
        try {
            Model m = entry.parse(files);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WrongProgramArgumentException e) {
            e.printStackTrace();
        } catch (InternalBackendException e) {
            e.printStackTrace();
        }

    }

    // implement refactoring i.e. Model -> transformed Model -> prettyprint & emit as file



}
