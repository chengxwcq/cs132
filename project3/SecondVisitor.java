import syntaxtree.*;
import visitor.*;
import java.util.*;

public class SecondVisitor extends GJDepthFirst<String, HashMap<String, String>> {

	private HashMap<String, LinkedList<LinkedList<String>>> classMap;
	private LinkedList<String> argumentsList;

	private int null_no = 1;
	private int if_no = 0;
	private int if_else_no = 1;
	private int while_no = 0;
	private int curid_no = 0;
	private int bounds_no = 1;

	private boolean isMainClass = false;
	private boolean AllocArray = false;

	private StringBuilder vapor;
	private String currentClass = null;
	private String callerName = null;

	// used in the new xx().method()
	private LinkedList<LinkedList<String>> cur_cl = null;
	// used when the type of the varDeclaration is some class
	private HashMap<String, String> fieldMap = new HashMap<>();

	public SecondVisitor(HashMap<String, LinkedList<LinkedList<String>>> classMap, StringBuilder vapor) {
		this.classMap = new HashMap<>(classMap);
		this.vapor = vapor;
	}

	/**
	 * Grammar production:
	 * f0 -> MainClass()
	 * f1 -> ( TypeDeclaration() )*
	 * f2 -> <EOF>
	 */
	public String visit(Goal g, HashMap<String, String> map) {
		isMainClass = true;
		g.f0.accept(this, map);
		isMainClass = false;
		vapor.append("\n");
		g.f1.accept(this, map);
		if (AllocArray) {
			vapor.append("func AllocArray(size)\nbytes = MulS(size 4) \nbytes = Add(bytes 4)\n");
			vapor.append("v = HeapAllocZ(bytes)\n[v] = size \nret v\n");
		}
		return null;
	}

	/**
	 * Grammar production:
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> "public"
	 * f4 -> "static"
	 * f5 -> "void"
	 * f6 -> "main"
	 * f7 -> "("
	 * f8 -> "String"
	 * f9 -> "["
	 * f10 -> "]"
	 * f11 -> Identifier()
	 * f12 -> ")"
	 * f13 -> "{"
	 * f14 -> ( VarDeclaration() )*
	 * f15 -> ( Statement() )*
	 * f16 -> "}"
	 * f17 -> "}"
	 */
	public String visit(MainClass mc, HashMap<String, String> map) {
		vapor.append("func Main()\n");
		HashMap<String, String> tmp = new HashMap<>();
		String var = mc.f14.accept(this, tmp);
		String sta = mc.f15.accept(this, tmp);
		vapor.append("ret\n");		
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> ( VarDeclaration() )*
	 * f4 -> ( MethodDeclaration() )*
	 * f5 -> "}"
	 */
	public String visit(ClassDeclaration cd, HashMap<String, String> map) {
		HashMap<String, String> m = new HashMap<>();
		currentClass = getIdentifier(cd.f1);
		cd.f3.accept(this, map);
		// store all the class FIELD information in the map
		LinkedList<String> fields = classMap.get(currentClass).get(1);
		for (int i = 0; i < fields.size(); i++) {
			m.put(fields.get(i), "[this+" + (i + 1) * 4 + "]");
		}
		cd.f4.accept(this, m);
		return currentClass;
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
	public String visit(ClassExtendsDeclaration ced, HashMap<String, String> map) {
		HashMap<String, String> m = new HashMap<>();
		currentClass = getIdentifier(ced.f1);
		ced.f5.accept(this, map);
		// store all the class FIELD information in the map
		LinkedList<String> fields = classMap.get(currentClass).get(1);
		for (int i = 0; i < fields.size(); i++) {
			m.put(fields.get(i), "[this+" + (i + 1) * 4 + "]");
		}
		ced.f5.accept(this, m);
		ced.f6.accept(this, m);
		return currentClass;	
	}

	/**
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
	public String visit(MethodDeclaration md, HashMap<String, String> map) {
		HashMap<String, String> fieldMap_copy = new HashMap<>(fieldMap);
		curid_no = 0;
		// m is used to store the argument of the method
		HashMap<String, String> m = new HashMap<>();
		String methodId = getIdentifier(md.f2);
		// append something   ******
		vapor.append("func " + currentClass + "." + methodId + "(this");
		md.f4.accept(this, m);

		for (String kk : m.keySet()) {
			vapor.append(" " + kk);
		}
		vapor.append(")\n");
		HashMap<String, String> allPara = new HashMap<>(map);
		for (String k : m.keySet()) {
			allPara.put(k, m.get(k));
		}
		// in the method content, use HashMap allPara  ********
		md.f7.accept(this, allPara);
		md.f8.accept(this, allPara);
		// q is the return state
		String q = md.f10.accept(this, allPara);
		String id = getS(curid_no++);
		String ff = map.getOrDefault(q, q);
		if (!ff.equals(q)) {
			vapor.append(id + " = " + ff + "\n");
			vapor.append("ret " + id + "\n\n");
		} else {
			vapor.append("ret " + q + "\n\n");
		}
		fieldMap = new HashMap<>(fieldMap_copy);
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> FormalParameter()
	 * f1 -> ( FormalParameterRest() )*
	 */
	public String visit(FormalParameterList fpl, HashMap<String, String> map) {
		fpl.f0.accept(this, map);
		fpl.f1.accept(this, map);
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> Type()
	 * f1 -> Identifier()
	 */
	public String visit(FormalParameter fp, HashMap<String, String> map) {
		String id = getIdentifier(fp.f1);
		if (fp.f0.f0.which == 3) {
			String f0 = fp.f0.f0.accept(this, map);
			fieldMap.put(id, f0);
		}
		map.put(id, id);
		return id;
	}

	/**
	 * Grammar production:
	 * f0 -> ","
	 * f1 -> FormalParameter()
	 */
	public String visit(FormalParameterRest fpr, HashMap<String, String> map) {
		String id = fpr.f1.accept(this, map);
		return id;
	}

	/**
	 * Grammar production:
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	public String visit(VarDeclaration vd, HashMap<String, String> map) {
		String id = getIdentifier(vd.f1);
		if (vd.f0.f0.which == 3) {
			String f0 = vd.f0.f0.accept(this, map);
			fieldMap.put(id, f0);
		}
		map.put(id, id);
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> Block()
	 *       | AssignmentStatement()
	 *       | ArrayAssignmentStatement()
	 *       | IfStatement()
	 *       | WhileStatement()
	 *       | PrintStatement()
	 */
	public String visit(Statement s, HashMap<String, String> map) {
		return s.f0.accept(this, map);
	}

	/**
	 * Grammar production:
	 * f0 -> "{"
	 * f1 -> ( Statement() )*
	 * f2 -> "}"
	 */
	public String visit(Block b, HashMap<String, String> map) {
		b.f1.accept(this, map);
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> "while"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 */
	public String visit(WhileStatement ws, HashMap<String, String> map) {
		String top = "while" + while_no + "_top";
		String end = "while" + (while_no++) + "_end";

		vapor.append(top + ":\n");
		String f2 = ws.f2.accept(this, map);
		vapor.append("if0 " + f2 + " goto :" + end + "\n");

		String f4 = ws.f4.accept(this, map);
		vapor.append("goto :" + top + "\n");
		vapor.append(end + ":\n");
		return ""; 
	}

	/**
 	* Grammar production:
	 * f0 -> Identifier()
	 * f1 -> "["
	 * f2 -> Expression()
	 * f3 -> "]"
	 * f4 -> "="
	 * f5 -> Expression()
	 * f6 -> ";"
	 */
	public String visit(ArrayAssignmentStatement aas, HashMap<String, String> map) {
		String f0 = aas.f0.accept(this, map);
		String f2 = aas.f2.accept(this, map);
		String f5 = aas.f5.accept(this, map);
		// id is the address of the array
		String id = getS(curid_no++);
		vapor.append(id + " = " + map.get(f0) + "\n");
		String nll = getNull(null_no++);
		vapor.append("if " + id + " goto :" + nll + "\n");
		vapor.append("Error(\"null pointer\")\n");
		vapor.append(nll + ":\n");

		String id2 = getS(curid_no++);
		vapor.append(id2 + " = [" + id + "+0]\n");
		vapor.append(id2 + " = Lt(" + f2 + " " + id2 + ")\n");
		String bound = getBound(bounds_no++);
		vapor.append("if " + id2 + " goto :" + bound + "\n");
		vapor.append("Error(\"array index out of bounds\")\n" + bound + ":\n");

		vapor.append(id2 + " = MulS(" + f2 + " 4)\n");
		vapor.append(id2 + " = Add(" + id2 + " " + id + ")\n");
		vapor.append("[" + id2 + "+4] = " + f5 + "\n");
		return "";
	}

	/**
 	* Grammar production:
	 * f0 -> "if"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 * f5 -> "else"
	 * f6 -> Statement()
	 */
	public String visit(IfStatement is, HashMap<String, String> map) {
		String f2 = is.f2.accept(this, map);
		String s = map.getOrDefault(f2, f2);
		if (!s.equals(f2)) {
			vapor.append(f2 + " = " + s + "\n");
		}

		String ifLabel = getIfLabel(if_no++);
		String ifElseLabel = getIfElseLabel(if_else_no);
		String ifEndLabel = getIfEndLabel(if_else_no++);
		vapor.append("if0" + " " + f2 + " goto :" + ifElseLabel + "\n");
		String f4 = is.f4.accept(this, map);
		vapor.append("goto :" + ifEndLabel + "\n" + ifElseLabel + ":\n");
		String f6 = is.f6.accept(this, map);
		vapor.append(ifEndLabel + ":\n");
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> Identifier()
	 * f1 -> "="
	 * f2 -> Expression()
	 * f3 -> ";"
	 */
	public String visit(AssignmentStatement as, HashMap<String, String> map) {
		String id = getIdentifier(as.f0);
		String vapor_id = map.get(id);
		String exp = as.f2.accept(this, map);
		if (map.containsKey(exp)) {
			exp = map.get(exp);
		}
		vapor.append(vapor_id + " = " + exp + "\n");
		vapor.append(id + " = " + vapor_id + "\n");
		return id;
	}

	/**
	 * Grammar production:
	 * f0 -> "System.out.println"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> ";"
	 */
	public String visit(PrintStatement ps, HashMap<String, String> map) {
		String s = ps.f2.accept(this, map);
		// will cause problem if f2 is an integer itself, not a variable 
		vapor.append("PrintIntS(" + s + ")\n");
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> AndExpression()
	 *       | CompareExpression()
	 *       | PlusExpression()
	 *       | MinusExpression()
	 *       | TimesExpression()
	 *       | ArrayLookup()
	 *       | ArrayLength()
	 *       | MessageSend()
	 *       | PrimaryExpression()
	 */
	public String visit(Expression e, HashMap<String, String> map) {
		return e.f0.accept(this, map);
	}

	/**
	 * Grammar production:
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> "length"
	 */
	public String visit(ArrayLength al, HashMap<String, String> map) {
		String f0 = al.f0.accept(this, map);
		String id = getS(curid_no++);
		vapor.append(id + " = [" + f0 + "+0]\n");
		String id2 = getS(curid_no++);
		vapor.append(id2 + " = " + id + "\n");
		return id2;
	}

	/**
	 * Grammar production:
	 * f0 -> PrimaryExpression()
	 * f1 -> "&&"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(AndExpression ae, HashMap<String, String> map) {
		String f0 = ae.f0.accept(this, map);
		String f2 = ae.f2.accept(this, map);

		String f0_ = map.getOrDefault(f0, f0);
		String f2_ = map.getOrDefault(f2, f2);
		if (!f0_.equals(f0)) {
			vapor.append(f0 + " = " + f0_ + "\n");
		}
		if (!f2_.equals(f2)) {
			vapor.append(f2 + " = " + f2_ + "\n");
		}

		String id = getS(curid_no++);
		vapor.append(id + " = MulS(" + f0 + " " + f2 + ")\n");
		return id;
	}

	/**
	 * Grammar production:
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	 */
	public String visit(ArrayLookup al, HashMap<String, String> map) {
		String f0 = al.f0.accept(this, map);
		String f2 = al.f2.accept(this, map);
		String p = map.get(f0);
		String id = getS(curid_no++);
		String id2 = getS(curid_no++);
		String id3 = getS(curid_no++);

		vapor.append(id + " = " + p + "\n");
		vapor.append(id2 + " = [" + id + "+0]\n");
		vapor.append(id2 + " = Lt(" + f2 + " " + id2 + ")\n");
		String bound = getBound(bounds_no++);
		vapor.append("if " + id2 + " goto :" + bound + "\n");
		vapor.append("Error(\"array index out of bounds\")\n" + bound + ":\n");

		vapor.append(id2 + " = MulS(" + f2 + " 4)\n");
		vapor.append(id2 + " = Add(" + id2 + " " + id + ")\n");
		vapor.append(id3 + " = [" + id2 + "+4]\n");

		return id3;	
	}

	/** binary expression
	 * Grammar production:
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(PlusExpression pe, HashMap<String, String> map) {
		String f0 = pe.f0.accept(this, map);
		String f2 = pe.f2.accept(this, map);
		return ExpressionHelper(f0, f2, "Add", map);
	}

	public String visit(CompareExpression ce, HashMap<String, String> map) {
		String f0 = ce.f0.accept(this, map);
		String f2 = ce.f2.accept(this, map);
		return ExpressionHelper(f0, f2, "LtS", map);
	}

	public String visit(MinusExpression me, HashMap<String, String> map) {
		String f0 = me.f0.accept(this, map);
		String f2 = me.f2.accept(this, map);
		return ExpressionHelper(f0, f2, "Sub", map);
	}

	public String visit(TimesExpression te, HashMap<String, String> map) {
		String f0 = te.f0.accept(this, map);
		String f2 = te.f2.accept(this, map);
		return ExpressionHelper(f0, f2, "MulS", map);
	}

	/**
	 * Grammar production:
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( ExpressionList() )?
	 * f5 -> ")"
	 */
	public String visit(MessageSend ms, HashMap<String, String> map) {
		// the result of PrimaryExpression() is stored in classId
		String classId = ms.f0.accept(this, map);
		callerName = map.getOrDefault(classId, classId);
		// System.out.println(callerName);
		String id = ms.f2.accept(this, map);
		LinkedList<String> vmd = null;

		// when f0 is the local variable
		if (fieldMap.containsKey(classId)) {
			String actualClass = fieldMap.get(classId);
			vmd = classMap.get(actualClass).get(0);
		} else {
			vmd = cur_cl.get(0);
		}
		int index = vmd.indexOf(id);
		String k = getS(curid_no);
			// what if the caller is not in the id - 1 ?
		if (callerName.equals(classId)) {
			vapor.append(k + " = [" + callerName + "]\n");
		} else {
			vapor.append(classId + " = " + callerName + "\n");
			vapor.append(k + " = [" + classId + "]\n");
		}
		vapor.append(k + " = [" + k + "+" + index * 4 + "]\n");
		curid_no++;

		argumentsList = new LinkedList<>();
		ms.f4.accept(this, map);
		String p = getS(curid_no++);
		
		vapor.append(p + " = call " + k + "(" + classId);
		for (String q : argumentsList) {
			vapor.append(" " + q);
		}
		vapor.append(")\n");
		return p;
	}

	/**
	 * Grammar production:
	 * f0 -> Expression()
	 * f1 -> ( ExpressionRest() )*
	 */
	public String visit(ExpressionList el, HashMap<String, String> map) {
		argumentsList = new LinkedList<>();
		// when used in arguments of the method
		String e = el.f0.accept(this, map);
		argumentsList.add(e);
		el.f1.accept(this, map);
		return "";
	}

	/**
	 * Grammar production:
	 * f0 -> ","
	 * f1 -> Expression()
	 */
	public String visit(ExpressionRest er, HashMap<String, String> map) {
		String e = er.f1.accept(this, map);
		argumentsList.add(e);
		return e;
	}

	/**
	 * Grammar production:
	 * f0 -> IntegerLiteral()
	 *       | TrueLiteral()
	 *       | FalseLiteral()
	 *       | Identifier()
	 *       | ThisExpression()
	 *       | ArrayAllocationExpression()
	 *       | AllocationExpression()
	 *       | NotExpression()
	 *       | BracketExpression()
	 */
	public String visit(PrimaryExpression pe, HashMap<String, String> map) {
		return pe.f0.accept(this, map);
	}

	/**
	 * Grammar production:
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	 */
	public String visit(ArrayAllocationExpression aae, HashMap<String, String> map) {
		AllocArray = true;
		String f3 = aae.f3.accept(this, map);
		String id = getS(curid_no++);
		vapor.append(id + " = call :AllocArray(" + f3 + ")\n");
		return id;
	}

	/**
	 * Grammar production:
	 * f0 -> "!"
	 * f1 -> Expression()
	 */
	public String visit(NotExpression ne, HashMap<String, String> map) {
		String s = ne.f1.accept(this, map);
		String id = getS(curid_no++);
		if (map.containsKey(s)) {
			vapor.append(s + " = " + map.get(s) + "\n");
		}
		vapor.append(id + " = Sub(1 " + s + ")\n");
		return id;
	}

	public String visit(Identifier i, HashMap<String, String> map) {
		// used in the MessageSend
		String id = getIdentifier(i);
		return id;
	}

	public String visit(TrueLiteral tl, HashMap<String, String> map) {
		return "1";
	}

	public String visit(FalseLiteral fl, HashMap<String, String> map) {
		return "0";
	}

	/**
	 * Grammar production:
	 * f0 -> "("
	 * f1 -> Expression()
	 * f2 -> ")"
	 */
	public String visit(BracketExpression be, HashMap<String, String> map) {
		return be.f1.accept(this, map);
	}

	/**
	 * Grammar production:
	 * f0 -> "this"
	 */
	public String visit(ThisExpression te, HashMap<String, String> map) {
		cur_cl = classMap.get(currentClass);
		return "this";
	}

	public String visit(IntegerLiteral il, HashMap<String, String> map) {
		return il.f0.toString();
	}

	/**
	 * Grammar production:
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	 */
	public String visit(AllocationExpression ae, HashMap<String, String> map) {
		String id = getIdentifier(ae.f1);
		cur_cl = classMap.get(id);

		// field length + one virtual method table
		int size = (cur_cl.get(1).size() + 1) * 4;
		String i = getS(curid_no);
		vapor.append(i + " = HeapAllocZ(" + size + ")\n");
		vapor.append("[" + i + "] = :vmt_" + id + "\n");

		String s = "null" + null_no++;
		vapor.append("if " + i + " goto :" + s + "\n");
		vapor.append("Error(\"null pointer\")\n" + s + ":\n");
		return "t." + (curid_no++);
	}

	/*************** helper function ****************/
	// get the name of the identifier
	private String getIdentifier(Identifier id) {
		return id.f0.toString();
	}

 	// used in the binary expression
 	// 1 + 3 => Add(1 3)
	private String ExpressionHelper(String f0, String f2, String operator, HashMap<String, String> map) {
		if (map.containsKey(f0) && !map.get(f0).equals(f0)) {
			String p = getS(curid_no++);
			vapor.append(p + " = " + map.get(f0) + "\n");
			f0 = p;
		}
		if (map.containsKey(f2) && !map.get(f2).equals(f2)) {
			String p = getS(curid_no++);
			vapor.append(p + " = " + map.get(f2) + "\n");
			f2 = p;
		}
		String result = getS(curid_no++);
		vapor.append(result + " = " + operator + "(" + f0 + " " + f2 + ")\n");
		return result;
	}

	// get the String from number.
	// ie. if the curid_no = 10, return "t.10"
	private String getS(int p) {
		return "t." + p;
	}

	private String getIfLabel(int p) {
		return "if" + p;
	}

	private String getIfElseLabel(int p) {
		return "if" + p + "_else";
	}

	private String getIfEndLabel(int p) {
		return "if" + p + "_end";
	}

	private String getNull(int p) {
		return "null" + p;
	}

	private String getBound(int p) {
		return "bounds" + p;
	}
}























