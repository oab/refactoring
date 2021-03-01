package refactoring;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;

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

    private class HideDelegateMatch extends Match {
        private VarUse line1AssignVar;
        private VarUse line2AssignVar;
        private VarUse line1CallVar;
        private VarUse line2CallVar;
        private SyncCall line1SyncCall;
        private SyncCall line2SyncCall;
        private AssignStmt line2AssignStmt;
        private List<Stmt> line2InStmts;
        //private InterfaceDecl usingI;
        //private ClassDecl usingC;

        public HideDelegateMatch(String inModule, String inClass, String inMethod,
                                 int line1, int line2) throws MatchException {
            ModuleDecl mDecl = model.lookupModule(inModule);
            if (mDecl == null) {
                throw new MatchException(String.format(pathError, "Module", inModule));
            }

            ClassDecl cDecl = (ClassDecl)mDecl.lookup(new KindedName(KindedName.Kind.CLASS, inClass));
            if(cDecl == null) {
                throw new MatchException(String.format(pathError, "Class", inClass));
            }

            MethodImpl mImpl = cDecl.lookupMethod(inMethod);
            if(mImpl == null) {
                throw new MatchException(String.format(pathError, "Method", inMethod));
            }

            List<Stmt> mstmts = mImpl.getBlockNoTransform().getStmtList();

            Stmt s1 = getStmtAtLine(mstmts,line1);
            if(s1 == null) throw new MatchException(String.format(lineNotFound,line1));
            Stmt s2 = getStmtAtLine(mstmts,line2);
            if(s2 == null) throw new MatchException(String.format(lineNotFound,line2));

            if(!(s1 instanceof AssignStmt))
                throw new MatchException(String.format(noAssigmStmt,line1));
            AssignStmt line1AssignStmt = (AssignStmt) s1;

            if(!(s2 instanceof AssignStmt))
                throw new MatchException(String.format(noAssigmStmt,line2));
            line2AssignStmt = (AssignStmt) s2;

            Exp line1AssignValue = line1AssignStmt.getValue();
            if(!(line1AssignValue instanceof SyncCall))
                throw new MatchException(String.format(noCall,line1));
            line1SyncCall = (SyncCall) line1AssignValue;

            Exp line2AssignValue = line2AssignStmt.getValue();
            if(!(line2AssignValue instanceof SyncCall))
                throw new MatchException(String.format(noCall,line2));
            line2SyncCall = (SyncCall) line2AssignValue;

            VarOrFieldUse line1AssignVarOrField =  line1AssignStmt.getVar();
            if(!(line1AssignVarOrField instanceof VarUse))
                throw new MatchException(String.format(expectingVar,line1));
            line1AssignVar = (VarUse) line1AssignVarOrField;

            VarOrFieldUse line2AssignVarOrField =  line2AssignStmt.getVar();
            if(!(line2AssignVarOrField instanceof VarUse))
                throw new MatchException(String.format(expectingVar,line2));
            line2AssignVar = (VarUse) line2AssignVarOrField;

            PureExp line1SyncallExp =  line1SyncCall.getCallee();
            if(!(line1SyncallExp instanceof VarUse))
                throw new MatchException(String.format(expectingVar,line2));
            line1CallVar = (VarUse) line1SyncallExp;

            PureExp line2SyncallExp =  line2SyncCall.getCallee();
            if(!(line2SyncallExp instanceof VarUse))
                throw new MatchException(String.format(expectingVar,line2));
            line2CallVar = (VarUse) line2SyncallExp;

            String var1 = line1AssignVar.getName();
            String var2 = line2CallVar.getName();

            if (!var1.equals(var2)) {
                throw new MatchException(String.format(varMissmatch, var1, line1, var2, line2));
            }
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
                        line2InStmts = nested;
                        break;
                    }
                }
                if (s.getStartLine() == line) {
                    out = s;
                    line2InStmts = stmts;
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
            line1AssignVar.setName(line2AssignVar.getName());
            line1SyncCall.setMethod(line2SyncCall.getMethod());
            line2InStmts.removeChild(line2AssignStmt);

            // Now we must ensure that
            // 1. the interface of callVar1 implements call2
            // 2. Any class implementing that interface implements call2




        }
    }


}
