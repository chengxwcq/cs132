import cs132.util.ProblemException;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.ast.*;

import java.util.*;

class GraphColoring {

	private String[] arguments = {"$a0", "$a1", "$a2", "$a3"};
	private String[] callee_saved = {"$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"};
	private String[] caller_saved = {"$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8"};
	private String[] temp_reg = {"$v0", "$v1"};
	private String[] local_reg;

	private int s_reg = 0;

	private HashMap<Integer, HashMap<String, VarNode>> rangeMap;
	private HashMap<Integer, HashSet<Integer>> callPosition;
	// used to store the information of the function
	private HashMap<Integer, ColoredNode> coloredMap;

	private HashMap<Integer, HashMap<String, String>> castMap;

	private HashMap<Integer, LinkedList<String>> argMap;

	// $t0 <= $a0, ....
	private HashMap<Integer, HashMap<String, String>> argToRegMap;

	public GraphColoring(HashMap<Integer, HashMap<String, VarNode>> rangeMap
						, HashMap<Integer, HashSet<Integer>> callPosition
						, HashMap<Integer, ColoredNode> coloredMap
						, HashMap<Integer, HashMap<String, String>> castMap
						, HashMap<Integer, LinkedList<String>> argMap
						, HashMap<Integer, HashMap<String, String>> argToRegMap) {
		this.rangeMap = rangeMap;
		this.callPosition = callPosition;
		this.coloredMap = coloredMap;
		this.castMap = castMap;
		this.argMap = argMap;
		this.argToRegMap = argToRegMap;
		local_reg = new String[100];
		for (int i = 0; i < local_reg.length; i++) {
			local_reg[i] = "local[" + (i + 8) + "]";
		}
	}

	public void coloring() {
		for (int p : rangeMap.keySet()) {
			HashMap<String, String> cast = new HashMap<>();
			castMap.put(p, cast);
			HashMap<String, String> a2g = new HashMap<>();
			argToRegMap.put(p, a2g);
			// used to count the max used s regs
			// to further calculate the local number
			int s_max_number = 0; 

			HashMap<String, VarNode> range = rangeMap.get(p);
			HashSet<Integer> callpos = callPosition.get(p);

			LinkedList<String> list = argMap.get(p);
			HashSet<String> args = new HashSet<>();

			for (int i = 0; i < list.size(); i++) {
				String arg = list.get(i);
				args.add(arg);
				if (i >= 4) {
					cast.put(arg, "in[" + (i - 4) + "]");
				} else {
					cast.put(arg, arguments[i]);
				}
			}

			// record the variables that need to be put into s stack
			// which is the variables whoes life range has a call
			HashSet<VarNode> sVariable = new HashSet<>();
			// put all the VarNodes in an array and sort
			VarNode[] nodeArr = new VarNode[range.size()];
			int index = 0;
			for (String s : range.keySet()) {
				VarNode node = range.get(s);
				for (int l : callpos) {
					if (inRange(node, l) && node.startPoint != l) {
						sVariable.add(node);
					}
				}
				nodeArr[index++] = node;
			}

			boolean[] s_used = new boolean[callee_saved.length];
			boolean[] t_used = new boolean[caller_saved.length];
			boolean[] local_used = new boolean[local_reg.length];

			for (int i = 0; i < list.size(); i++) {
				String arg = list.get(i);
				String aOrIn = cast.get(arg);

				VarNode node = range.get(arg);
				boolean flag = true;
				if (!sVariable.contains(node)) {
					int free = getFreeReg(t_used);
					if (free != -1) {
						t_used[free] = true;
						a2g.put(aOrIn, caller_saved[free]);
						continue;
					}
				} else {
					int free = getFreeReg(s_used);
					if (free != -1) {
						s_used[free] = true;
						a2g.put(aOrIn, callee_saved[free]);
					} else {
						free = getFreeReg(local_used);
						local_used[free] = true;
						a2g.put(aOrIn, local_reg[free]);
					}
				}
			}
			
			Arrays.sort(nodeArr, (a, b) -> a.startPoint - b.startPoint);

			// for (int i = 0; i < nodeArr.length; i++) {
			// 	VarNode aaa = nodeArr[i];
			// 	System.out.println(aaa.name + " " + aaa.startPoint + " " + aaa.endPoint);
			// }

			HashSet<String> obsolete = new HashSet<>();
			for (int i = 0; i < nodeArr.length; i++) {
				// free the regs whose life has already end
				for (int j = i - 1; j >= 0; j--) {
					if (obsolete.contains(nodeArr[j].name)) {
						continue;
					}
					// < or <=
					if (nodeArr[j].endPoint <= nodeArr[i].startPoint) { 
						String reg = cast.get(nodeArr[j].name);
						// System.out.println(nodeArr[j].endPoint + "   " + reg + "   " + nodeArr[j].name);
						if (getIndex(caller_saved, reg) != -1) {
							t_used[getIndex(caller_saved, reg)] = false;
						} else if (getIndex(callee_saved, reg) != -1) {
							s_used[getIndex(callee_saved, reg)] = false;
						} else if (getIndex(local_reg, reg) != -1) {
							local_used[getIndex(local_reg, reg)] = false;
						}
						obsolete.add(nodeArr[j].name);
					}
				}
				// means this variable is an argument
				if (args.contains(nodeArr[i].name)) {
					continue;
				}
				VarNode curNode = nodeArr[i];
				boolean putIntoTReg = false;
				if (!sVariable.contains(curNode)) {
					int aa = getFreeReg(t_used);
					if (aa != -1) {
						putIntoTReg = true;
						int ix = getFreeReg(t_used);
						cast.put(curNode.name, caller_saved[ix]);
						t_used[ix] = true;
					}
				}
				// means that this variable should be put into s stack
				if (!putIntoTReg) {
					int count = 0;
					int aa = getFreeReg(s_used);
					if (aa != -1) {
						s_used[aa] = true;
						cast.put(curNode.name, callee_saved[aa]);
					} else { // have to put into local stack
						int ix = getFreeReg(local_used);
						cast.put(curNode.name, local_reg[ix]);
						local_used[ix] = true;
					}
					for (int jj = 0; jj < s_used.length; jj++) {
						if (s_used[jj]) count++;
					}
					for (int jj = 0; jj < local_used.length; jj++) {
						if (local_used[jj]) count++;
					}
					s_max_number = Math.max(s_max_number, count);
				}
			}
			coloredMap.get(p).local = s_max_number;
		}
	}


	private boolean inRange(VarNode node, int line) {
		int start = node.startPoint;
		int end = node.endPoint;
		return start <= line && line < end;
	}

	// return the first avaliable t register and return the index
	// if all are used, just return -1
	private int getFreeReg(boolean[] used) {
		for (int i = 0; i < used.length; i++) {
			if (!used[i]) return i;
		}
		return -1;
	}

	private int getIndex(String[] p, String target) {
		for (int i = 0; i < p.length; i++) {
			if (target.equals(p[i])) {
				return i;
			}
		}
		return -1;
	}
}
































