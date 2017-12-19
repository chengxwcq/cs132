import syntaxtree.*;
import visitor.*;
import java.util.*;

public class FirstVisitor extends GJVoidDepthFirst<LinkedList<String>> {

	private HashMap<String, LinkedList<LinkedList<String>>> map;
	private HashMap<String, LinkedList<LinkedList<String>>> vaporMap;

	public FirstVisitor(HashMap<String, LinkedList<LinkedList<String>>> map, 
					HashMap<String, LinkedList<LinkedList<String>>> vaporMap) {
		this.map = map;
		this.vaporMap = vaporMap;
	}

	/** Goal
	 * Grammar production:
	 * f0 -> MainClass()
	 * f1 -> ( TypeDeclaration() )*
	 * f2 -> <EOF>
	 */
	public void visit(Goal g, LinkedList<String> list) {
		g.f1.accept(this, list);
	}

	/** TypeDeclaration
	 * Grammar production:
	 * f0 -> ClassDeclaration()
	 *       | ClassExtendsDeclaration()
	 */
	public void visit(TypeDeclaration td, LinkedList<String> list) {
		td.f0.accept(this, list);
	}

	/** ClassDeclaration
	 * Grammar production:
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> ( VarDeclaration() )*
	 * f4 -> ( MethodDeclaration() )*
	 * f5 -> "}"
	 */
	public void visit(ClassDeclaration cd, LinkedList<String> list) {
		String className = getIdentifier(cd.f1);
		LinkedList<String> field = new LinkedList<>();
		LinkedList<String> vmd = new LinkedList<>();

		cd.f3.accept(this, field);
		cd.f4.accept(this, vmd);

		LinkedList<String> vaporField = new LinkedList<>();
		LinkedList<String> vaporVmd = new LinkedList<>();

		for (String p : field) {
			vaporField.add(className + "." + p);
		}

		for (String p : vmd) {
			vaporVmd.add(className + "." + p);
		}

		LinkedList<LinkedList<String>> a = new LinkedList<>();
		a.add(new LinkedList<>(vmd));
		a.add(new LinkedList<>(field));

		LinkedList<LinkedList<String>> b = new LinkedList<>();
		b.add(new LinkedList<>(vaporVmd));
		b.add(new LinkedList<>(vaporField));
		map.put(className, a);
		vaporMap.put(className, b);
	}

	/**
	 * Grammar production:
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "extends"
	 * f3 -> Identifier()
	 * f4 -> "{"
	 * f5 -> ( VarDeclaration() )*
	 * f6 -> ( MethodDeclaration() )*
	 * f7 -> "}"
	 */
	public void visit(ClassExtendsDeclaration ced, LinkedList<String> list) {
		String className = getIdentifier(ced.f1);
		String BaseClassName = getIdentifier(ced.f3);
		
		LinkedList<LinkedList<String>> p = map.get(BaseClassName);
		LinkedList<LinkedList<String>> q = vaporMap.get(BaseClassName);

		LinkedList<String> tmp_vmd = new LinkedList<>();
		LinkedList<String> tmp_field = new LinkedList<>();

		ced.f5.accept(this, tmp_field);
		ced.f6.accept(this, tmp_vmd);

		LinkedList<String> base_vmd = p.get(0);
		LinkedList<String> base_field = p.get(1);

		LinkedList<String> vmd = new LinkedList<>(base_vmd);
		for (String x : tmp_vmd) {
			if (!vmd.contains(x)) {
				vmd.add(x);
			}
		}

		LinkedList<String> field = new LinkedList<>(base_field);
		for (String x : tmp_field) {
			field.add(x);
		}


		// LinkedList<String> field = new LinkedList<>();
		LinkedList<String> vapor_vmd = new LinkedList<>(q.get(0));
		for (String s : tmp_vmd) {
			if (base_vmd.contains(s)) {
				int index = base_vmd.indexOf(s);
				vapor_vmd.set(index, className + "." + s);
			} else {
				vapor_vmd.add(className + "." + s);
			}
		}
		LinkedList<String> vapor_field = new LinkedList<>();
		LinkedList<LinkedList<String>> a = new LinkedList<>();
		a.add(vapor_vmd);
		a.add(vapor_field);
		vaporMap.put(className, a);


		LinkedList<LinkedList<String>> kk = new LinkedList<>();
		kk.add(new LinkedList<>(vmd));
		kk.add(new LinkedList<>(field));
		map.put(className, kk);
	}

	/** VarDeclaration
	 * Grammar production:
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	public void visit(VarDeclaration vd, LinkedList<String> list) {
		String s = getIdentifier(vd.f1);
		if (!list.contains(s)) {
			list.add(s);
		}
	}

	/** MethodDeclaration
	 * Grammar production:
	 * f0 -> "public"
	 * f1 -> Type()
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( FormalParameterList() )?
	 * f5 -> ")"
	 * f6 -> "{"
	 * f7 -> ( VarDeclaration() )*
	 * f8 -> ( Statement() )*
	 * f9 -> "return"
	 * f10 -> Expression()
	 * f11 -> ";"
	 * f12 -> "}"
	 */
	public void visit(MethodDeclaration md, LinkedList<String> list) {
		String s = getIdentifier(md.f2);
		if (!list.contains(s)) {
			list.add(s);
		}
	}

	/*************** helper function ****************/
	// get the name of the identifier
	private String getIdentifier(Identifier id) {
		return id.f0.toString();
	}
}






