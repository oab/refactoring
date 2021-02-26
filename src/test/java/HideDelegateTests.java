import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;

import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.junit.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.parser.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.KindedName.Kind;
import refactoring.HideDelegate;
import refactoring.Refactor;


import static refactoring.HideDelegate.*;

public class HideDelegateTests {

	/*
	 * For these tests we will assume we want to pick the snippet
	 * 1. d = p.getDept();
	 * 2. m = d.getManager();
     */

	/* TODO: JUnit has a pattern for a set of input files,
	     actually we want pairs of (filename, Match), since
			 each file may have its own exact incantation of the
			 refactoring.
	*/
	String plain = "examples/hideDelegate/plain.abs";
	String block = "examples/hideDelegate/block.abs";
	String interleaved = "examples/hideDelegate/interleaved.abs";
	String defused = "examples/hideDelegate/def-used.abs";
	String inModule = "HideDelegate";
	String inClass = "Client";
	String inMethod = "enquire";

	@Test
	public void astTest() throws Exception {
		Main entry = new Main();
		Model m = entry.parse(Collections.singletonList(new File(plain)));
		assert (m!=null);
		ModuleDecl mod = m.lookupModule(inModule);
		assert (mod!=null);
		ClassDecl c = (ClassDecl)mod.lookup(new KindedName(Kind.CLASS,inClass));
		assert (c!=null);
		MethodImpl md = c.lookupMethod(inMethod);
		assert (md!=null);
		int n = 5;
		Stmt s1 = md.getBlock().getStmt(n);
		assertThat(s1, instanceOf(AssignStmt.class));
		Stmt s2 = md.getBlock().getStmt(n+1);
		assertThat(s2, instanceOf(AssignStmt.class));
		int s1absoluteLine = s1.getStartLine();
		int s2absoluteLine = s2.getStartLine();
		assertThat(s1absoluteLine,equalTo(52));
		assertThat(s2absoluteLine,equalTo(53));

	}

	@Test
	public void hideDelegatePlainTest() throws Exception {
		String file = plain;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file+".after";
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		Refactor r = new HideDelegate(inModule,inClass,inMethod,52);
		r.refactor(in);

		in.doPrettyPrint(writer,formatter);

		Model out = entry.parse(Collections.singletonList(new File(outFile)));
		/* TODO: There's a warning in the JavaDoc here, also maybe check the
			       ABS unit-tests on what's the best way to check for correct output. */
		assertFalse(out.hasErrors());
	}

	@Test
	public void hideDelegateBlockTest() throws Exception {
		String file = block;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file+".after";
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		Refactor r = new HideDelegate(inModule,inClass,inMethod,53);
		r.refactor(in);
		in.doPrettyPrint(writer,formatter);
		Model out = entry.parse(Collections.singletonList(new File(outFile)));
		assertFalse(out.hasErrors());
	}

	@Test
	public void interleavedTest() throws Exception {
		String file = interleaved;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file+".after";
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		/* TODO: I think it's okay of there's a hacky way to create Match
		         from the sample just for this test here (or via line numbers, or ...)
    
		Match m = Pattern1Match(...);
		Refactor r = new HideDelegate(m);
		r.refactor(in);
		in.doPrettyPrint(writer,formatter);
		*/
	}
}
