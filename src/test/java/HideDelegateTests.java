package refactoring;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.abs_models.backend.prettyprint.ABSFormatter;
import org.abs_models.backend.prettyprint.DefaultABSFormatter;

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
	String methodpresent = "examples/hideDelegate/methodpresent.abs";
	String block = "examples/hideDelegate/block.abs";
	String interleaved = "examples/hideDelegate/interleaved.abs";
	String makemethod = "examples/hideDelegate/makemethod.abs";
	String makemethodclash = "examples/hideDelegate/makemethodclash.abs";


	@Test
	public void astTest() throws Exception {
		Main entry = new Main();
		Model m = entry.parse(Collections.singletonList(new File("examples/hideDelegate/methodpresent.abs")));
		assert (m!=null);
		ModuleDecl mod = m.lookupModule("HideDelegate");
		assert (mod!=null);
		ClassDecl c = (ClassDecl)mod.lookup(new KindedName(Kind.CLASS,"Client"));
		assert (c!=null);
		MethodImpl md = c.lookupMethod("enquire");
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
		Model m = entry.parse(Collections.singletonList(new File(methodpresent)));
		List<CompilationUnit> cunits = m.getCompilationUnitList();

		PrintWriter writer = new PrintWriter(System.out);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		SourcePosition found = null;
		for (CompilationUnit cunit : cunits) {
			String name = cunit.getFileName();

			if(name.equals(methodpresent)) {
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


	@ParameterizedTest
	@MethodSource("testFiles")
	public void hideDelegateTest(String name, int line1, int line2) throws Exception {
		String file = "examples/hideDelegate/" + name;

		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(new File(file)));
		assert (in!=null);
		String outFile = file.replaceFirst(".abs","-after.abs");
		String expectedFile = file.replaceFirst(".abs","-after-expected.abs");
		PrintWriter writer = new PrintWriter(outFile);
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		HideDelegateMatch m = HideDelegate.getMatch(in,"HideDelegate","Client","enquire",line1,line2);
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

	private static Stream<Arguments> testFiles() {
		return Stream.of(
				Arguments.of("methodpresent.abs", 52,53),
				Arguments.of("block.abs", 53, 54),
				Arguments.of("interleaved.abs",52,54),
				Arguments.of("makemethod.abs",44,45),
				Arguments.of("makemethodclash.abs",47,48)
		);
	}

}
