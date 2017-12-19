import cs132.util.ProblemException;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.ast.*;

import java.util.*;

class PrintVisitor extends VInstr.Visitor<RuntimeException> {
	
	HashSet<String> buildInFunction = new HashSet<>();
	// StringBuilder dataSegment = new StringBuilder();
	// StringBuilder textSegment = new StringBuilder();
	VaporProgram vp;
	// used to store _str0: .asciiz "null pointer\n" ....
	HashMap<String, String> strStorage = new HashMap<>();

	StringBuilder textSegment = new StringBuilder();
	HashSet<String> definedFuntion = new HashSet<>();
	HashMap<Integer, String> labelMap;
	int strNum = 0;
	int outStackNum = 0;

	boolean canPrint = true;

	public PrintVisitor(VaporProgram vp) {
		this.vp = vp;
		// .....
	}

	private void println(String s) {
		if (canPrint) {
			System.out.println(s);
		}
	}

	private void print(String s) {
		if (canPrint) {
			System.out.print(s);
		}
	}

	public void printDataSegment() {
		StringBuilder dataSegment = new StringBuilder();
		dataSegment.append(".data\n\n");
		for (VDataSegment ds : vp.dataSegments) {
			VOperand.Static[] vs = ds.values;
			dataSegment.append(ds.ident + ":\n");
			for (VOperand.Static v : vs) {
				dataSegment.append(v.toString().substring(1) + "\n");
			}
		}
		println(dataSegment.toString());
	}

	public void printTextSegment() {
		textSegment.append(".text\n\n");
		textSegment.append("jal Main\nli $v0 10\nsyscall\n\n");

		for (VFunction function : vp.functions) {
			labelMap = new HashMap<>();
			VCodeLabel[] labels = function.labels;
			for (VCodeLabel label : labels) {
				int line = label.sourcePos.line;
				String l = label.ident;
				labelMap.put(line, l);
			}

			VFunction.Stack stack = function.stack;
			textSegment.append(function.ident + ":\n");
			textSegment.append("sw $fp -8($sp)\nmove $fp $sp\n");
			int num = (2 + stack.local + stack.out) * 4;
			textSegment.append("subu $sp $sp " + num + "\n");
			textSegment.append("sw $ra -4($fp)\n");
			outStackNum = stack.out;

			for (VInstr instr : function.body) {
				for (int ss = instr.sourcePos.line - 1; labelMap.containsKey(ss); ss--) {
					if (labelMap.containsKey(ss)) {
						textSegment.append(labelMap.get(ss) + ":\n");
					}
				}	
				instr.accept(this);
			}
			textSegment.append("lw $ra -4($fp)\n");
			textSegment.append("lw $fp -8($fp)\n");
			textSegment.append("addu $sp $sp " + num + "\n");
			textSegment.append("jr $ra\n");
			textSegment.append("\n");
		}

		for (String s : definedFuntion) {
			if (s.equals("_print")) {
				textSegment.append("_print:\nli $v0 1   # syscall: print integer\nsyscall\n");
				textSegment.append("la $a0 _newline\nli $v0 4   # syscall: print string\n");
				textSegment.append("syscall\njr $ra\n\n");
			} else if (s.equals("_error")) {
				textSegment.append("_error:\nli $v0 4   # syscall: print string\nsyscall\n");
				textSegment.append("li $v0 10  # syscall: exit\nsyscall\n\n");
			} else if (s.equals("_heapAlloc")) {
				textSegment.append("_heapAlloc:\nli $v0 9   # syscall: sbrk\n");
				textSegment.append("syscall\njr $ra\n\n");
			}
		}

		textSegment.append(".data\n.align 0\n_newline: .asciiz \"\\n\"\n");

		for (String str : strStorage.keySet()) {
			String string = strStorage.get(str);
			textSegment.append(string + ": .asciiz " + str.substring(0, str.length() - 1) + "\\n\"\n");
		}

		System.out.println(textSegment.toString());
	}	

	public void visit(VAssign b) {
		VVarRef dest = b.dest;
		VOperand source = b.source;
		String dest_string = dest.toString();
		String source_string = source.toString();
		if (source_string.charAt(0) != ':') {
			textSegment.append((getVOperand(source) ? "li " : "move ") + dest_string + " " + source_string + "\n");	
		} else {
			textSegment.append("la " + dest_string + " " + source_string.substring(1) + "\n");
		}
	}

	public void visit(VBranch b) {
		String if_s = b.positive ? "bnez " : "beqz ";
		String value = b.value.toString();
		textSegment.append(if_s + value + " " + b.target.ident + "\n");
	}

	public void visit(VBuiltIn b) {
		VBuiltIn.Op op = b.op;
		// Add Sub MulS
		// Eq Lt LtS
		// PrintIntS
		// HeapAllocZ
		// Error
		String dest = null;
		if (b.dest != null) {
			dest = b.dest.toString();
		}

		String arg1 = null;
		String arg2 = null;
		boolean a = false;
		boolean c = false;

		if (b.args.length == 1) {
			arg1 = b.args[0].toString();
			a = getVOperand(b.args[0]);
		} else if (b.args.length == 2) {
			arg1 = b.args[0].toString();
			arg2 = b.args[1].toString();
			a = getVOperand(b.args[0]);
			c = getVOperand(b.args[1]);
		}

		switch (op.name) {
			case "Add":
				if (a && c) {
					textSegment.append("li " + dest + " " + (Integer.valueOf(arg1) + Integer.valueOf(arg2)) + "\n"); 
				} else if (a || c) {
					String ss = a ? arg1 : arg2;
					textSegment.append("li $t9 " + Integer.valueOf(ss) + "\n");
					textSegment.append("addu " + dest + " " + (a ? arg2 : arg1) + " $t9\n");
				} else {
					textSegment.append("addu " + dest + " " + arg1 + " " + arg2  + "\n");
				}
				break;

			case "Sub":
				if (a && c) {
					textSegment.append("li " + dest + " " + (Integer.valueOf(arg1) - Integer.valueOf(arg2)) + "\n"); 
				} else if (a || c) {
					String ss = a ? arg1 : arg2;
					textSegment.append("li $t9 " + Integer.valueOf(ss) + "\n");
					textSegment.append("subu " + dest + " " + (a ? arg2 : arg1) + " $t9\n");
				} else {
					textSegment.append("subu " + dest + " " + arg1 + " " + arg2  + "\n");
				}
				break;

			case "MulS":
				if (a && c) {
					textSegment.append("li " + dest + " " + (Integer.valueOf(arg1) * Integer.valueOf(arg2)) + "\n"); 
				} else if (a || c) {
					String ss = a ? arg1 : arg2;
					textSegment.append("li $t9 " + Integer.valueOf(ss) + "\n");
					textSegment.append("mul " + dest + " " + (a ? arg2 : arg1) + " $t9\n");
				} else {
					textSegment.append("mul " + dest + " " + arg1 + " " + arg2  + "\n");
				}
				break;

			case "LtS":
				if (c) {
					textSegment.append("slti " + dest + " " + arg1 + " " + arg2 + "\n");
				} else {
					textSegment.append("slt " + dest + " " + arg1 + " " + arg2 + "\n");
				}
				break;

			case "Lt":
				if (a) {
					textSegment.append("li $t9 " + arg1 + "\n");
					textSegment.append("sltu " + dest + " $t9 " + arg2 + "\n");
				} else {
					textSegment.append("sltu " + dest + " " + arg1 + " " + arg2 + "\n");
				}
				break;

			case "PrintIntS":
				definedFuntion.add("_print");
				String t = "move";
				if (a) {
					t = "li";
				}
				textSegment.append(t + " $a0 " + arg1 + "\njal _print\n");
				break;

			case "HeapAllocZ":
				definedFuntion.add("_heapAlloc");
				if (a) {
					textSegment.append("li $a0 " + arg1 + "\njal _heapAlloc\n");
					textSegment.append("move " + dest + " $v0\n");
				} else {
					textSegment.append("move $a0 " + arg1 + "\njal _heapAlloc\n");
					textSegment.append("move " + dest + " $v0\n");
				}	
			break; 

			case "Error":
				definedFuntion.add("_error");
				if (strStorage.containsKey(arg1)) {
					textSegment.append("la $a0 " + strStorage.get(arg1) + "\nj _error\n");
				} else {
					strStorage.put(arg1, "_str" + strNum);
					textSegment.append("la $a0 _str" + strNum++ + "\nj _error\n");
				}
				break;

			default:
				throw new RuntimeException("buildInFunction not defined " + op.name);
		}
	}

	public void visit(VCall b) {
		VAddr<VFunction> addr = b.addr;
		String k = addr.toString();
		if (addr instanceof VAddr.Label) {
			k = k.substring(1);
			textSegment.append("jal " + k + "\n");
		} else {
			textSegment.append("jalr " + k + "\n");
		}			
	}


	public void visit(VGoto b) {
		textSegment.append("j " + b.target.toString().substring(1) + "\n");
	}


	public void visit(VMemRead b) {
		String dest = b.dest.toString();
		String source = getVMemRef(b.source);
		textSegment.append("lw " + dest + " " + source + "\n");
	}


	public void visit(VMemWrite b) {
		String dest = getVMemRef(b.dest);
		String source = b.source.toString();
		if (source.charAt(0) == ':') {
			textSegment.append("la $t9 " + source.substring(1) + "\n");
			textSegment.append("sw $t9 " + dest + "\n");
		} else {
			if (getVOperand(b.source)) {
				source = "$t9";
				textSegment.append("li $t9 " + b.source.toString() + "\n");
			}
			textSegment.append("sw " + source + " " + dest + "\n");
		}
	}


	public void visit(VReturn b) {
		return;
	}

	private boolean getVOperand(VOperand v) {
		return (v instanceof VOperand.Static);
	}

	private String getVMemRef(VMemRef source) {
		if (source instanceof VMemRef.Global) {
			VMemRef.Global p = (VMemRef.Global) source;
			return p.byteOffset + "(" + p.base.toString() + ")";
		} else if (source instanceof VMemRef.Stack) {
			VMemRef.Stack vmem = (VMemRef.Stack) source;
			VMemRef.Stack.Region region = vmem.region;
			String s = region.toString();
			if (s.equals("In")) {
				return vmem.index * 4 + "($fp)";
			} else if (s.equals("Out")) {
				return vmem.index * 4 + "($sp)";
			} else {
				return (vmem.index + outStackNum)* 4 + "($sp)";
			}
		} else {
			throw new RuntimeException("something wrong with VMemRef");
		}
	}
}






















