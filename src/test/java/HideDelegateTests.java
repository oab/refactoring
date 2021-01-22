import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;

import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.junit.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.parser.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.KindedName.Kind;


import static refactoring.Refactor.*;

public class HideDelegateTests {

	/*
	 * For these tests we will assume we want to pick the snippet
	 * 1. d = p.getDept();
	 * 2. m = d.getManager();
     */

	String sample = "examples/hideDelegate/before.abs";
	String modName = "HideDelegate";
	String className = "Client";
	String metName = "enquire";
	String aVar = "d";
	String tVar = "p";
	String mCall = "getDept";


	@Test
	public void test1() throws Exception {
		Main entry = new Main();
		Model m = entry.parse(Collections.singletonList(new File(sample)));
		assert (m!=null);
		ModuleDecl mod = m.lookupModule(modName);
		assert (mod!=null);
		ClassDecl c = (ClassDecl)mod.lookup(new KindedName(Kind.CLASS,className));
		assert (c!=null);
		MethodImpl md = c.lookupMethod(metName);
		assert (md!=null);
		int n = 5;
		Stmt s1 = md.getBlock().getStmt(n);
		assertThat(s1, instanceOf(AssignStmt.class));
		Stmt s2 = md.getBlock().getStmt(n+1);
		assertThat(s2, instanceOf(AssignStmt.class));
		return;
	}

	@Test
	public void test2() throws Exception {
		Main entry = new Main();
		Model m = entry.parse(Collections.singletonList(new File(sample)));
		PrintWriter writer = new PrintWriter(new File(sample+".after"));
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		hideDelegate(m,modName,className,metName,aVar,tVar,mCall)
				.doPrettyPrint(writer,formatter);
	}
}
