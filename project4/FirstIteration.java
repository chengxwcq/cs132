import cs132.util.ProblemException;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.ast.*;

import java.util.*;

class FirstIteration extends VInstr.Visitor<RuntimeException>{

	VaporProgram vp = null;
	// this first Integer is to store the function id
	// second HashMap is used to store all the live range of the variables
	// in the function
	HashMap<Integer, HashMap<String, VarNode>> rangeMap;
	HashMap<String, VarNode> nodeMap = null;
	// function_start is used to deal with the arguments case,
	// cause there is no assign operation in the function body
	int function_start = 0; 
	// apart from this, we should record the location of each function call,
	// since when a function call is in the life range of a varible, we have 
	// to store this variable into s register, since this variable is likely
	// to change if we put them into t register.
	HashMap<Integer, HashSet<Integer>> callPosition;
	HashSet<Integer> callSet;
	// used to store the information of in, out.
	HashMap<Integer, ColoredNode> coloredMap;
	ColoredNode cn;

	HashMap<String, Integer> gotoMap;

	// used to store the arguments of a function;
	HashMap<Integer, LinkedList<String>> argMap;

	public FirstIteration(VaporProgram vp, HashMap<Integer, HashMap<String, VarNode>> rangeMap
						, HashMap<Integer, HashSet<Integer>> callPosition, HashMap<Integer, ColoredNode> coloredMap
						, HashMap<Integer, LinkedList<String>> argMap) {
		this.vp = vp;
		this.rangeMap = rangeMap;
		this.callPosition = callPosition;
		this.coloredMap = coloredMap;
		this.argMap = argMap;
	}

	public void getLifeRange() {
		VFunction[] functions = vp.functions;
		for (VFunction function : functions) {

			// get the while loop in this function and use this 
			// to extend the variable endpoint if the variable
			// if defined before while and used in while
			// labelMap is used to store the label name and label line
			HashMap<String, Integer> labelMap = new HashMap<>();
			gotoMap = new HashMap<>();
			VCodeLabel[] labels = function.labels;
			for (VCodeLabel label : labels) {
				labelMap.put(label.ident, label.sourcePos.line);
			}

			int index = function.index;
			cn = new ColoredNode();
			LinkedList<String> list = new LinkedList<>();

			for (int i = 0; i < function.params.length; i++) {
				list.add(function.params[i].toString());
			}
			argMap.put(index, list);
			int paramsLength = function.params.length;
			cn.in = paramsLength > 4 ? paramsLength - 4 : 0; 

			function_start = function.sourcePos.line;
			nodeMap = new HashMap<>();
			callSet = new HashSet<>();
			for (String var : function.vars) {
				VarNode tmp = new VarNode();
				tmp.setName(var);
				nodeMap.put(var, tmp);
			}
			VInstr[] body = function.body;
			for (VInstr instr : body) {
				instr.accept(this);
			}

			// deal with gotoMap and labelMap
			// HashMap<String, Integer>
			for (String gt : gotoMap.keySet()) {
				if (labelMap.containsKey(gt)) {
					int gt_line = gotoMap.get(gt);
					int lb_line = labelMap.get(gt);
					// means this is a while loop
					if (lb_line < gt_line) {
						for (String nodeName : nodeMap.keySet()) {
							VarNode node = nodeMap.get(nodeName);
							if (node.startPoint < lb_line 
							 && node.endPoint > lb_line 
							 && node.endPoint < gt_line) {
								node.endPoint = gt_line;
							}
						}
					}
				}
			}

			rangeMap.put(function.index, nodeMap);
			callPosition.put(function.index, callSet);
			coloredMap.put(index, cn);
		}
	}

	public void visit(VAssign a) {
		int line = a.sourcePos.line;
		// first assign here
		String assign = a.dest.toString();
		VarNode t = nodeMap.get(assign);
		if (t.startPoint == 0) {
			t.setStartPoint(line);
			// t.setEndPoint(line + 1);
		}
	}

	public void visit(VBranch b) {
		int line = b.sourcePos.line;
		setVVarRef(b.value, line);

		String s = b.target.toString();
		if (s.length() > 1) {
			gotoMap.put(s.substring(1), b.sourcePos.line);
		}
	}

	public void visit(VBuiltIn b) {
		int line = b.sourcePos.line;
		VOperand[] args = b.args;

		// some builtin function doesn't have a dest, such as PrintIntS
		if (b.dest != null) {
			String dest = b.dest.toString();
			if (nodeMap.containsKey(dest)) {
				VarNode node = nodeMap.get(dest);
				// first define
				if (node.startPoint == 0) {
					node.setStartPoint(line);
					// node.setEndPoint(line + 1);
				}
			}
		}

		for (VOperand arg : args) {
			setVVarRef(arg, line);
		}
	}

	public void visit(VCall c) {
		int line = c.sourcePos.line;
		callSet.add(line);
		VOperand[] args = c.args;
		if (args.length > 4) {
			cn.out = Math.max(cn.out, args.length - 4);
		}
		
		String dest = c.dest.toString();
		if (nodeMap.containsKey(dest)) {
			VarNode node = nodeMap.get(dest);
			// first define
			if (node.startPoint == 0) {
				node.setStartPoint(line);
				// node.setEndPoint(line);
			}
		}
		
		for (VOperand arg : args) {
			setVVarRef(arg, line);
		}

		VAddr<VFunction> addr = c.addr;
		setVAddrVar(addr, line);
	}

	public void visit(VGoto g) {
		String s = g.target.toString();
		if (s.length() > 1) {
			gotoMap.put(s.substring(1), g.sourcePos.line);
		}
	}

	public void visit(VMemRead m) {
		int line = m.sourcePos.line;
		String dest = m.dest.toString();
		// first use
		if (nodeMap.containsKey(dest)) {
			VarNode node = nodeMap.get(dest);
			// first define
			if (node.startPoint == 0) {
				node.setStartPoint(line);
				// node.setEndPoint(line + 1);
			}
		}

		VMemRef source = m.source;
		if (source instanceof VMemRef.Global) {
			VAddr<VDataSegment> tmp = ((VMemRef.Global) source).base;
			setVAddrVar(tmp, line);
		}
	}

	public void visit(VMemWrite m) {
		int line = m.sourcePos.line;
		VMemRef dest = m.dest;

		if (dest instanceof VMemRef.Global) {
			VAddr<VDataSegment> tmp = ((VMemRef.Global) dest).base;
			setVAddrVar(tmp, line);
		}

		VOperand source = m.source;
		setVVarRef(source, line);
	}

	public void visit(VReturn m) {
		int line = m.sourcePos.line;
		VOperand value = m.value;
		setVVarRef(value, line);
	}

	private void setVVarRef(VOperand oper, int line) {
		if (oper instanceof VVarRef) {
			String name = ((VVarRef) oper).toString();
			// use of this variable
			VarNode node = nodeMap.get(name);
			// no assignment means this variable is argument
			if (node.startPoint == 0) {
				node.setStartPoint(function_start);
				// node.setEndPoint(function_start + 1);
			}
			node.setEndPoint(line);
		}
	}

	private void setVAddrVar(VAddr addr, int line) {
		if (addr instanceof VAddr.Var) {
			String var = ((VAddr.Var) addr).var.toString();
			VarNode node = nodeMap.get(var);
			if (node.startPoint == 0) {
				node.setEndPoint(function_start + 1);
				// node.setStartPoint(function_start);
			}
			node.setEndPoint(line);
		}
	}
}

















