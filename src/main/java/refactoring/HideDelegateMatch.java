package refactoring;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;


// A match here is when bracketed parts are equal
// [delegateVar] = callVar1.m1
// assignVar2 = [callVar2].m2
public class HideDelegateMatch {

    static String pathError    = "%s by name %s not found";
    static String lineNotFound = "No statement at line %d";
    static String noAssigmStmt = "Statement at line %d is not an assignment statement";
    static String noCall       = "Righthand side of assignmnt statement at line %d is not a call";
    static String varMissmatch = "Variable %s used in assignment at line %d " +
            "not the same as the variable %s used in call at line %d";
    static String expectingVar = "expecting variable use, not field use at line %d";
    static String expectingI =   "expecting interface type for variable %s in line %d";


    VarUse delegateVar;
    VarUse serverVar;
    VarUse delegateCallVar;
    VarUse serverCallVar;
    SyncCall serverCall;
    SyncCall delegateCall;
    AssignStmt delegateStmt;
    List<Stmt> delegateStmtList;
    InterfaceDecl serverI;
    ArrayList<ClassDecl> serverC;

    HideDelegateMatch(Model m, String file, int line1, int line2) {

    }

    HideDelegateMatch(Model model, String inModule, String inClass, String inMethod,
                             int line1, int line2) throws MatchException {

        ModuleDecl mDecl = model.lookupModule(inModule);
        if (mDecl == null) {
            throw new MatchException(String.format(pathError, "Module", inModule));
        }

        ClassDecl cDecl = (ClassDecl) mDecl.lookup(new KindedName(KindedName.Kind.CLASS, inClass));
        if (cDecl == null) {
            throw new MatchException(String.format(pathError, "Class", inClass));
        }

        MethodImpl mImpl = cDecl.lookupMethod(inMethod);
        if (mImpl == null) {
            throw new MatchException(String.format(pathError, "Method", inMethod));
        }

        List<Stmt> mstmts = mImpl.getBlockNoTransform().getStmtList();

        AssignStmt serverStmt = cast(getStmtAtLine(mstmts, line1), AssignStmt.class, noAssigmStmt, line1);
        delegateStmt = cast(getStmtAtLine(mstmts, line2), AssignStmt.class, noAssigmStmt, line2);

        serverCall = cast(serverStmt.getValue(), SyncCall.class, noCall, line1);

        delegateCall = cast(this.delegateStmt.getValue(), SyncCall.class, noCall, line2);

        delegateVar = cast(serverStmt.getVar(), VarUse.class, expectingVar, line1);

        serverVar = cast(this.delegateStmt.getVar(), VarUse.class, expectingVar, line2);

        delegateCallVar = cast(serverCall.getCallee(), VarUse.class, expectingVar, line2);

        serverCallVar = cast(delegateCall.getCallee(), VarUse.class, expectingVar, line2);

        if (!delegateVar.getName().equals(serverCallVar.getName())) {
            throw new MatchException(String.format(varMissmatch,
                    delegateVar.getName(), line1, serverCallVar.getName(), line2));
        }

        serverI = cast(delegateCallVar.getType().getDecl(),InterfaceDecl.class,
                expectingI, delegateCallVar.getName(), line1);

        serverC = findImplementingClasses(serverI, mDecl.getDecls());
    }

    private <From, To extends From> To cast(From o, Class<To> tclass, String error, Object... args)
    throws MatchException{
        if(tclass.isInstance(o)) {
            return tclass.cast(o);
        }

        throw new MatchException(String.format(error,args));
    }

    private ArrayList<ClassDecl> findImplementingClasses(InterfaceDecl idecl, List<Decl> decls) {
        ArrayList<ClassDecl> found = new ArrayList<>();
        for (Decl decl : decls) {

            if (decl instanceof ClassDecl) {
                ClassDecl cdecl = (ClassDecl) decl;
                Collection<InterfaceDecl> implifs = cdecl.getDirectSuperTypes();
                if (implifs.contains(idecl)) {
                    found.add(cdecl);
                }
            }
        }
        return found;
    }

    private Stmt getStmtAtLine(List<Stmt> stmts, int line) throws MatchException {
        Stmt out = getStmtAtLineImpl(stmts,line);
        if(out == null) {
            throw new MatchException(String.format(lineNotFound, line));
        }
        return out;
    }

    private Stmt getStmtAtLineImpl(List<Stmt> stmts, int line) {
        Stmt out = null;
        for (Stmt s : stmts) {
            if (s instanceof Block) {
                Block b = (Block) s;
                List<Stmt> nested = b.getStmtList();

                Stmt nestout = getStmtAtLineImpl(nested, line);
                if (nestout != null) {
                    out = nestout;
                    delegateStmtList = nested;
                    break;
                }
            }
            if (s.getStartLine() == line) {
                out = s;
                delegateStmtList = stmts;
                break;
            }
        }
        return out;
    }

}
