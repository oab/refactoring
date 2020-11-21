import java.io.File;
import java.util.Collections;
import org.junit.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.parser.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.KindedName.Kind;

public class HideDelegateTests {

	String sample = "examples/hideDelegate/before.abs";
	String modName = "HideDelegate";
	String cName = "Client";
	String mName = "enquire";

	@Test
	public void test1() throws Exception {
		Main entry = new Main();
		Model m = entry.parse(Collections.singletonList(new File(sample)));
		assert (m!=null);
		ModuleDecl mod = m.lookupModule(modName);
		assert (mod!=null);
		ClassDecl c = (ClassDecl)mod.lookup(new KindedName(Kind.CLASS,cName));
		assert (c!=null);
		MethodImpl md = c.lookupMethod(mName);
		assert (md!=null);
		int n = 5;
		Stmt s1 = md.getBlock().getStmt(n);
		assertThat(s1, instanceOf(AssignStmt.class));
		Stmt s2 = md.getBlock().getStmt(n+1);
		assertThat(s2, instanceOf(AssignStmt.class));
		return;
	}
}
