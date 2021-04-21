package refactoring;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;
import org.junit.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.parser.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.KindedName.Kind;

import javax.xml.transform.Source;

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
	String makemethod = "examples/hideDelegate/makemethod.abs";
	String makemethodclash = "examples/hideDelegate/makemethodclash.abs";
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

   /* Notes: This was tested in hopes that we could pick out through the SourcePosition.findPosition
	  some ASTNode at a (line,column). This might not be the node we are looking for as we do not know
	  ahead of time at which column some root node that we are looking for starts. Therefore to use this
	  we need to check all possible columns. In that case using this we are doing strictly more work calling
	  this function on each column rather than just iterating through the statement at this line, gotten through
	  other means

	@Test
     public void compilationUnitTest() throws Exception {
		Main entry = new Main();
		Model m = entry.parse(Collections.singletonList(new File(plain)));
		List<CompilationUnit> cunits = m.getCompilationUnitList();

		PrintWriter writer = new PrintWriter(System.out);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		SourcePosition found = null;
		for (CompilationUnit cunit : cunits) {
			String name = cunit.getFileName();

			if(name.equals(plain)) {
				System.out.println(name);

				for(int i=1;i<23;i++) {
					found = SourcePosition.findPosition(cunit, 52, i);
					System.out.println(found.getContextNode().getStartColumn());
					System.out.println(found.getContextNode().toString());
				}
			}
		}

		assert (found != null);
		assertThat(found.getContextNode(),instanceOf(AssignStmt.class));

	 }

	 */


	@Test
	public void hideDelegatePlainTest() throws Exception {
		String file = plain;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file.replaceFirst(".abs","-after.abs");
		String expectedFile = file.replaceFirst(".abs","-after-expected.abs");
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		HideDelegateMatch m = HideDelegate.getMatch(in,inModule,inClass,inMethod,52,53);
		HideDelegate.refactor(m);

		in.doPrettyPrint(writer,formatter);
		Model out = entry.parse(Collections.singletonList(new File(outFile)));
		assertFalse(out.hasErrors());
		/* TODO: Avoid reading in again; also apache.commons may have something better. */
		Path expectedPath = Path.of("", "").resolve(expectedFile);
        	String expectedContent = Files.readString(expectedPath);
		Path outPath = Path.of("", "").resolve(outFile);
        	String outContent = Files.readString(outPath);
		assertThat(outContent.toString(), is(expectedContent));
	}

	@Test
	public void hideDelegateBlockTest() throws Exception {
		String file = block;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file.replaceFirst(".abs","-after.abs");
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		HideDelegateMatch m = HideDelegate.getMatch(in,inModule,inClass,inMethod,53,54);
		HideDelegate.refactor(m);

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
		String outFile = file.replaceFirst(".abs","-after.abs");

		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		/* TODO: Still redundant. For a unit-test, probably just the line numbers
							are input enough, and the constructor only matches the structure.
							If you want to be safe, you'd afterwards match names. */

		HideDelegateMatch m = HideDelegate.getMatch(in,inModule,inClass,inMethod,52,54);
		HideDelegate.refactor(m);

	    //assertEquals("d",m.assignVar1);
		//assertEquals("d",((VarOrFieldUse) ((SyncCall) m.syncallstmt2).getCallee()).getName());
		/* TODO: I think it's okay of there's a hacky way to create Match
		         from the sample just for this test here (or via line numbers, or ...) */

		in.doPrettyPrint(writer,formatter);
		Model out = entry.parse(Collections.singletonList(new File(outFile)));
		assertFalse(out.hasErrors());

	}

	@Test
	public void makemethodTest() throws Exception {
		String file = makemethod;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file.replaceFirst(".abs","-after.abs");
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		HideDelegateMatch m = HideDelegate.getMatch(in,inModule,inClass,inMethod,44,45);
		assert(m != null);
		HideDelegate.refactor(m);

		in.doPrettyPrint(writer,formatter);
		Model out = entry.parse(Collections.singletonList(new File(outFile)));
		assertFalse(out.hasErrors());
	}

	@Test
	public void makemethodclashTest() throws Exception {
		String file = makemethodclash;
		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file.replaceFirst(".abs","-after.abs");
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		HideDelegateMatch m = HideDelegate.getMatch(in,inModule,inClass,inMethod,47,48);
		assert(m != null);
		HideDelegate.refactor(m);

		in.doPrettyPrint(writer,formatter);
		Model out = entry.parse(Collections.singletonList(new File(outFile)));
		assertFalse(out.hasErrors());
	}

}
