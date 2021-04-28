package refactoring;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

public class HideDelegateTests {

	/*
	 * For these tests we will assume we want to pick the snippet
	 * 1. d = p.getDept();
	 * 2. m = d.getManager();
     */

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

   /* Note: This was tested in hopes that we could pick out through the SourcePosition.findPosition
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
	public void hideDelegateTest(String file, int line1, int line2) throws Exception {

		String outFile = file.replaceFirst(".abs","-after.abs");
		String expectedFile = file.replaceFirst(".abs","-after-expected.abs");

		Path inPath = Path.of("examples","hideDelegate",file);
		Path outPath = Path.of("examples", "hideDelegate").resolve(outFile);
		Path expectedPath = Path.of("examples", "hideDelegate").resolve(expectedFile);

		Main entry = new Main();
		Model in = entry.parse(Collections.singletonList(inPath.toFile()));

		PrintWriter writer = new PrintWriter(outPath.toFile());
		ABSFormatter formatter = new DefaultABSFormatter(writer);

		HideDelegateMatch m = HideDelegate.getMatch(in,"HideDelegate","Client","enquire",line1,line2);
		HideDelegate.refactor(m);

		in.doPrettyPrint(writer,formatter);
		Model out = entry.parse(Collections.singletonList(outPath.toFile()));
		assertFalse(out.hasErrors());

		/* TODO: Avoid reading in again; also apache.commons may have something better. */
        String expectedContent = Files.readString(expectedPath);
		String outContent = Files.readString(outPath);

		assertTrue(stripWsEq(expectedContent,outContent));

	}

	// Note: Remember to not put any comments in any expected files
	private static boolean stripWsEq(String a, String b) {
		String x = a.replaceAll("\\s+","");
		String y = b.replaceAll("\\s+","");
		return x.equals(y);
	}

	private static Stream<Arguments> testFiles() {
		return Stream.of(
				Arguments.of("methodpresent.abs", 52,53),
				Arguments.of("block.abs", 53, 54),
				Arguments.of("interleaved.abs",52,54),
				Arguments.of("makemethod.abs",44,45),
				Arguments.of("makemethodclash.abs",46,47)
		);
	}

}
