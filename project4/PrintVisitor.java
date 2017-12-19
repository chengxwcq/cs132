import cs132.util.ProblemException;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.ast.*;

import java.util.*;

class PrintVisitor extends VInstr.Visitor<RuntimeException> {
	private HashMap<Integer, ColoredNode> coloredMap;
	private HashMap<Integer, HashMap<String, String>> castMap;
	private HashMap<Integer, HashMap<String, String>> argToRegMap;
	private VaporProgram vp;
	private StringBuilder sb = new StringBuilder();

	private HashMap<String, String> cast;
	private HashMap<Integer, String> labelMap;
	private HashMap<String, String> a2g;
	private String[] callee_saved = {"$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"};
	private String[] arguments = {"$a0", "$a1", "$a2", "$a3"};

	private String pushback = "";

	PrintVisitor(HashMap<Integer, ColoredNode> coloredMap
			   , HashMap<Integer, HashMap<String, String>> castMap
			   , VaporProgram vp
			   , HashMap<Integer, HashMap<String, String>> argToRegMap) {
		this.coloredMap = coloredMap;
		this.castMap = castMap;
		this.vp = vp;
		this.argToRegMap = argToRegMap;
	}

	public void printVMT() {
		VDataSegment[] dataSegments = vp.dataSegments;
		for (VDataSegment dataSegment : dataSegments) {
			VOperand.Static[] vs = dataSegment.values;
			System.out.println("const " + dataSegment.ident);
			for (VOperand.Static v : vs) {
				System.out.println(v.toString());
			}
		}
		System.out.println();
	}

	public void printBody() {
		for (VFunction function : vp.functions) {
			int indexOfFunc = function.index;

			labelMap = new HashMap<>();
			VCodeLabel[] labels = function.labels;
			for (VCodeLabel label : labels) {
				int line = label.sourcePos.line;
				String l = label.ident;
				labelMap.put(line, l);
			}

			cast = castMap.get(indexOfFunc);
			ColoredNode cn = coloredMap.get(indexOfFunc);

			String inOutLocal = "in " + cn.in + ", out " + cn.out + ", local " + cn.local; 
			sb.append("func " + function.ident + " [" + inOutLocal + "]\n");
			String test = "";
			for (int i = 0; i < Math.min(cn.local, callee_saved.length); i++) {
				sb.append("local[" + i + "] = " + callee_saved[i] + "\n");
			}

			a2g = argToRegMap.get(indexOfFunc);
			for (String p : a2g.keySet()) {
				sb.append(a2g.get(p) + " = " + p + "\n");
			}

			// something wrong with pushback
			pushback = "";
			for (int i = 0; i < Math.min(cn.local, callee_saved.length); i++) {
				pushback += (callee_saved[i] + " = " + "local[" + i + "]\n");
			}

			VInstr[] body = function.body;
			for (VInstr b : body) {
				for (int ss = b.sourcePos.line - 1; labelMap.containsKey(ss); ss--) {
					if (labelMap.containsKey(ss)) {
						sb.append(labelMap.get(ss) + ":\n");
					}
				}	
			 	b.accept(this);
			}
			sb.append("\n");
		}

		System.out.println(sb.toString());
	}

	public void visit(VAssign a) {
		String dest = getVVarRefName(a.dest);
		String source = getVOperandName(a.source);
		sb.append(dest + " = " + source + "\n");
	}

	public void visit(VBranch a) {
		String target = a.target.toString();
		String value = getVOperandName(a.value);
		String op = "";
		if (a.positive) {
			op = "if";
		} else {
			op = "if0";
		}
		sb.append(op + " " + value + " goto " + target + "\n");
	}

	public void visit(VBuiltIn a) {
		String[] args = new String[a.args.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = getVOperandName(a.args[i]);
		}

		VVarRef dest = a.dest;
		String op = a.op.name;
		if (dest != null) {
			String name = getVVarRefName(a.dest);
			sb.append(name + " = ");
		}

		if (args.length > 1) {
			sb.append(op + "(" + args[0]);
			for (int i = 1; i < args.length; i++) {
				sb.append(" " + args[i]);
			}
			sb.append(")\n");
		} else if (op.equals("Error")) {
			sb.append("Error(\"" + args[0] + "\")\n");
		} else if (args.length == 1) {
			sb.append(op + "(" + args[0] + ")\n");
		} else {
			System.out.println("Error: the args length is 0");
		}
	}

	public void visit(VCall a) {
		String dest = cast.getOrDefault(a.dest.ident, "null");
		VAddr<VFunction> addr = a.addr;
		String adr;
		if (addr instanceof VAddr.Label) {
			adr = ((VAddr.Label) addr).toString();
		} else {
			adr = getVVarRefName(((VAddr.Var) addr).var);
		}

		String[] args = new String[a.args.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = getVOperandName(a.args[i]);
		}

		for (int i = 0; i < args.length; i++) {
			if (i < arguments.length) {
				sb.append(arguments[i] + " = " + args[i] + "\n");
			} else {
				sb.append("out[" + (i - 4) + "] = " + args[i] + "\n");
			}
		}

		sb.append("call " + adr + "\n");
		sb.append(dest + " = " + "$v0\n");
	}

	public void visit(VGoto a) {
		sb.append("goto " + a.target.toString() + "\n");
	}

	public void visit(VMemRead a) {
		String dest = getVVarRefName(a.dest);
		VMemRef source = a.source;
		String src = "";
		if (source instanceof VMemRef.Global) {
			VMemRef.Global ins = (VMemRef.Global) source;
			String t = cast.getOrDefault(ins.base.toString(), "null");
			if (a2g.containsKey(t)) {
				t = a2g.get(t);
			}
			src = "[" + t + "+" + ins.byteOffset + "]";
		} else {
			System.out.println("vmemread error");
		}
		sb.append(dest + " = " + src + "\n");
	}

	public void visit(VMemWrite a) {
		String src = "";
		VMemRef dest = a.dest;
		if (dest instanceof VMemRef.Global) {
			VMemRef.Global ins = (VMemRef.Global) dest;
			String t = cast.getOrDefault(ins.base.toString(), "null");
			if (a2g.containsKey(t)) {
				t = a2g.get(t);
			}
			src = "[" + t + "+" + ins.byteOffset + "]";
		} else {
			System.out.println("vmemread error");
		}

		String source = getVOperandName(a.source);
		sb.append(src + " = " + source + "\n");
	}

	public void visit(VReturn a) {
		if (a.value == null) {
			sb.append(pushback + "ret\n");
			return;
		}
		String value = getVOperandName(a.value);
		sb.append("$v0 = " + value + "\n" + pushback + "ret\n");
	}

	private String getVVarRefName(VVarRef dest) {
		String p = cast.getOrDefault(dest.toString(), "null");
		if (a2g.containsKey(p)) {
			return a2g.get(p);
		} else {
			return p;
		}
	}

	private String getVOperandName(VOperand vo) {
		if (vo instanceof VVarRef) {
			return getVVarRefName((VVarRef) vo);
		} else if (vo instanceof VLitStr) {
			return ((VLitStr) vo).value;
		} else {
			VOperand.Static v = (VOperand.Static) vo;
			if (v instanceof VLabelRef) {
				return ":" + ((VLabelRef) v).ident;
			} else {
				return ((VLitInt) v).value + "";
			}
		}
	}
}	


























