package refactoring;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.Type;

import java.util.ArrayList;
import java.util.Collection;


// A match here is when bracketed parts are equal
// [assignVar1] = callVar1.m1
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

    // Split this up?
    protected HideDelegateMatch(Model model, String inModule, String inClass, String inMethod,
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

        Stmt serverStmt = getStmtAtLine(mstmts, line1);
        if (serverStmt == null) throw new MatchException(String.format(lineNotFound, line1));

        Stmt delegateStmt = getStmtAtLine(mstmts, line2);
        if (delegateStmt == null) throw new MatchException(String.format(lineNotFound, line2));


        if (!(serverStmt instanceof AssignStmt))
            throw new MatchException(String.format(noAssigmStmt, line1));
        AssignStmt line1AssignStmt = (AssignStmt) serverStmt;

        if (!(delegateStmt instanceof AssignStmt))
            throw new MatchException(String.format(noAssigmStmt, line2));
        this.delegateStmt = (AssignStmt) delegateStmt;

        Exp line1AssignValue = line1AssignStmt.getValue();
        if (!(line1AssignValue instanceof SyncCall))
            throw new MatchException(String.format(noCall, line1));
        serverCall = (SyncCall) line1AssignValue;

        Exp line2AssignValue = this.delegateStmt.getValue();
        if (!(line2AssignValue instanceof SyncCall))
            throw new MatchException(String.format(noCall, line2));
        delegateCall = (SyncCall) line2AssignValue;

        VarOrFieldUse line1AssignVarOrField = line1AssignStmt.getVar();
        if (!(line1AssignVarOrField instanceof VarUse))
            throw new MatchException(String.format(expectingVar, line1));
        delegateVar = (VarUse) line1AssignVarOrField;

        VarOrFieldUse line2AssignVarOrField = this.delegateStmt.getVar();
        if (!(line2AssignVarOrField instanceof VarUse))
            throw new MatchException(String.format(expectingVar, line2));
        serverVar = (VarUse) line2AssignVarOrField;

        PureExp line1SyncallExp = serverCall.getCallee();
        if (!(line1SyncallExp instanceof VarUse))
            throw new MatchException(String.format(expectingVar, line2));
        delegateCallVar = (VarUse) line1SyncallExp;

        PureExp line2SyncallExp = delegateCall.getCallee();
        if (!(line2SyncallExp instanceof VarUse))
            throw new MatchException(String.format(expectingVar, line2));
        serverCallVar = (VarUse) line2SyncallExp;

        String var1 = delegateVar.getName();
        String var2 = serverCallVar.getName();

        if (!var1.equals(var2)) {
            throw new MatchException(String.format(varMissmatch, var1, line1, var2, line2));
        }

        serverI = findInterface(delegateCallVar, line1);
        serverC = findImplementingClasses(serverI, mDecl.getDecls());
        InterfaceDecl delegateI = findInterface(serverCallVar, line2);
    }

    private InterfaceDecl findInterface(VarUse v, int l) throws MatchException {
        Type t = v.getType();
        Decl d = t.getDecl();
        if (!(d instanceof InterfaceDecl))
            throw new MatchException(String.format(expectingI, v.getName(), l));
        return (InterfaceDecl) d;
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

    private Stmt getStmtAtLine(List<Stmt> stmts, int line) {
        Stmt out = null;
        for (Stmt s : stmts) {
            if (s instanceof Block) {
                Block b = (Block) s;
                List<Stmt> nested = b.getStmtList();

                Stmt nestout = getStmtAtLine(nested, line);
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
