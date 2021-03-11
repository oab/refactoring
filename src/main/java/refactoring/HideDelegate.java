package refactoring;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.Type;

import java.util.ArrayList;
import java.util.Collection;

// TODO: Split finding instances/first instance from actual refactoring?

// Matching against e.g
// 1. d = p.getDept();
// 2. m = d.getManager();
// Assume p also implements getManager (TODO assume -> ensure)
// remove 1. and 2. and replace with 3.
// 3. m = p.getManager();

public class HideDelegate extends Refactoring {

    public HideDelegate(Model m) {
        super(m);
    }

    public Match getMatch(String inModule, String inClass, String inMethod,
                          int line1, int line2) throws MatchException {
        return new HideDelegateMatch(inModule,inClass,inMethod,line1,line2);
    }

   // A match here is when bracketed parts are equal
   // [assignVar1] = callVar1.m1
   // assignVar2 = [callVar2].m2

    static String pathError    = "%s by name %s not found";
    static String lineNotFound = "No statement at line %d";
    static String noAssigmStmt = "Statement at line %d is not an assignment statement";
    static String noCall       = "Righthand side of assignmnt statement at line %d is not a call";
    static String varMissmatch = "Variable %s used in assignment at line %d " +
                                 "not the same as the variable %s used in call at line %d";
    static String expectingVar = "expecting variable use, not field use at line %d";
    static String expectingI =   "expecting interface type for variable %s in line %d";

    private class HideDelegateMatch extends Match {
        private VarUse delegateVar;
        private VarUse serverVar;
        private VarUse delegateCallVar;
        private VarUse serverCallVar;
        private SyncCall serverCall;
        private SyncCall delegateCall;
        private AssignStmt delegateStmt;
        private List<Stmt> delegateStmtList;
        private InterfaceDecl serverI;
        private ArrayList<ClassDecl> serverC;

        // Split this up?
        public HideDelegateMatch(String inModule, String inClass, String inMethod,
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

            serverI = findInterface(delegateCallVar,line1);
            serverC = findImplementingClasses(serverI,mDecl.getDecls());
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
            for (Decl decl : decls){

                if(decl instanceof ClassDecl) {
                    ClassDecl cdecl = (ClassDecl) decl;
                    Collection<InterfaceDecl> implifs = cdecl.getDirectSuperTypes();
                    if(implifs.contains(idecl)) {
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

        public  void refactor() {
            // Perform transformation
            // Pre :
            // assignVar1 = callVar1.call1(...)
            // assignVar2 = callVar2.call2(...)
            // Post:
            // assignVar2 = callVar1.call2(...)
            delegateVar.setName(serverVar.getName());
            serverCall.setMethod(delegateCall.getMethod());
            delegateStmtList.removeChild(delegateStmt);

            // Now we must ensure that
            // 1. the interface of callVar1 implements call2
            // 2. Any class implementing that interface implements call2

            // Add the delegate call to the server interface if not present
            if(serverI.lookupMethod(delegateCall.getMethodSig().getName()) == null) {
                serverI.addBody(delegateCall.getMethodSig().copy());
            }
            // for all classes implementing the delegate call insert the  needed method body
            for(ClassDecl cdecl : serverC) {
                if(cdecl.lookupMethod(delegateCall.getMethod()) == null) {
                    MethodSig sig = delegateCall.getMethodSig().copy();
                    cdecl.addMethod(makeMethod(sig,serverCall.getMethodSig().copy()));
                }
            }
        }

        // TODO: must ensure temporaries are unbound wherever this is inserted
        private MethodImpl makeMethod(MethodSig sig1, MethodSig sig2) {
            String temp1="temp1";
            String temp2="temp2";
            InterfaceTypeUse tu1 = new InterfaceTypeUse(sig2.getReturnType().getName(), new List<>());
            InterfaceTypeUse tu2 = new InterfaceTypeUse(sig1.getReturnType().getName(), new List<>());
            SyncCall call1 = new SyncCall(new VarUse("this"),sig2.getName(), new List<>());
            SyncCall call2 = new SyncCall(new VarUse(temp1),sig1.getName(), new List<>());
            VarDecl decl1 = new VarDecl(temp1, tu1, new Opt<Exp>(call1));
            VarDecl decl2 = new VarDecl(temp2, tu2, new Opt<Exp>(call2));
            VarDeclStmt stmt1 = new VarDeclStmt(new List<>(),decl1);
            VarDeclStmt stmt2 = new VarDeclStmt(new List<>(),decl2);
            ReturnStmt stmt3 = new ReturnStmt(new List<>(),new VarUse(temp2));
            return new MethodImpl(sig1,new Block().addStmt(stmt1).addStmt(stmt2).addStmt(stmt3));
        }


    }


}
