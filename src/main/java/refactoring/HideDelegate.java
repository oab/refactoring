package refactoring;

import org.abs_models.frontend.ast.*;

// Matching against e.g
// 1. d = p.getDept();
// 2. m = d.getManager();
// remove 1. and 2. and replace with 3.
// 3. m = p.getManager();

public class HideDelegate {

    public static HideDelegateMatch getMatch(Model m,String inModule, String inClass, String inMethod,
                          int line1, int line2) throws MatchException {
        return new HideDelegateMatch(m, inModule,inClass,inMethod,line1,line2);
    }

    // one could imagine getMatches -> [matches], but beware performing a refactoring
    // will then invalidate the other matches.


    public static void refactor(HideDelegateMatch match) {
        // Perform transformation
        // Pre :
        // assignVar1 = callVar1.call1(...)
        // assignVar2 = callVar2.call2(...)
        // Post:
        // assignVar2 = callVar1.call2(...)
        match.delegateVar.setName(match.serverVar.getName());
        match.serverCall.setMethod(match.delegateCall.getMethod());
        match.delegateStmtList.removeChild(match.delegateStmt);

        // Now we must ensure that
        // 1. the interface of callVar1 implements call2
        // 2. Any class implementing that interface implements call2

        // Add the delegate call to the server interface if not present
        if (match.serverI.lookupMethod(match.delegateCall.getMethodSig().getName()) == null) {
            match.serverI.addBody(match.delegateCall.getMethodSig().copy());
        }
        // for all classes implementing the delegate call insert the  needed method body
        for (ClassDecl cdecl : match.serverC) {
            if (cdecl.lookupMethod(match.delegateCall.getMethod()) == null) {
                MethodSig sig = match.delegateCall.getMethodSig().copy();
                // should these names perhaps be taken from the matching lines
                String name1 = "temp1";
                String name2 = "temp2";

                while(cdecl.locallookupVarOrFieldName(name1,false) != null) {
                    name1 += "_";
                }

                while(cdecl.locallookupVarOrFieldName(name2,false) != null) {
                    name2 += "_";
                }
                cdecl.addMethod(makeMethod(sig, match.serverCall.getMethodSig().copy(),name1,name2));
            }
        }
        // If there are no calls
    }

    private static MethodImpl makeMethod(MethodSig sig1, MethodSig sig2, String temp1, String temp2) {
        InterfaceTypeUse tu1 = new InterfaceTypeUse(sig2.getReturnType().getName(), new List<>());
        InterfaceTypeUse tu2 = new InterfaceTypeUse(sig1.getReturnType().getName(), new List<>());
        SyncCall call1 = new SyncCall(new VarUse("this"), sig2.getName(), new List<>());
        SyncCall call2 = new SyncCall(new VarUse(temp1), sig1.getName(), new List<>());
        VarDecl decl1 = new VarDecl(temp1, tu1, new Opt<Exp>(call1));
        VarDecl decl2 = new VarDecl(temp2, tu2, new Opt<Exp>(call2));
        VarDeclStmt stmt1 = new VarDeclStmt(new List<>(), decl1);
        VarDeclStmt stmt2 = new VarDeclStmt(new List<>(), decl2);
        ReturnStmt stmt3 = new ReturnStmt(new List<>(), new VarUse(temp2));
        return new MethodImpl(sig1, new Block().addStmt(stmt1).addStmt(stmt2).addStmt(stmt3));
    }
}
