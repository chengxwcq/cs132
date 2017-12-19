import cs132.util.ProblemException;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.ast.*;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;

import java.util.*;

public class V2VM {

	public static void main(String[] args) {
		try {
			VaporProgram vp = parseVapor(System.in, System.out);
			HashMap<Integer, HashMap<String, VarNode>> map = new HashMap<>();
			HashMap<Integer, HashSet<Integer>> callPosition = new HashMap<>();
			HashMap<Integer, ColoredNode> coloredMap = new HashMap<>();
			HashMap<Integer, LinkedList<String>> argMap = new HashMap<>();
			HashMap<Integer, HashMap<String, String>> argToRegMap = new HashMap<>();

			FirstIteration fi = new FirstIteration(vp, map, callPosition, coloredMap, argMap);
			fi.getLifeRange();

			HashMap<Integer, HashMap<String, String>> castMap = new HashMap<>();
			GraphColoring gc = new GraphColoring(map, callPosition, coloredMap, castMap, argMap, argToRegMap);
			gc.coloring();

			PrintVisitor pv = new PrintVisitor(coloredMap, castMap, vp, argToRegMap);

			pv.printVMT();
			pv.printBody();

			// for (VFunction function : vp.functions) {
			// 	VCodeLabel[] labels = function.labels;
			// 	for (VCodeLabel label : labels) {
			// 		System.out.println(label.ident);
			// 	}
			// 	System.out.println();
			// }

			// for (int a : castMap.keySet()) {
			// 	HashMap<String, String> aa = castMap.get(a);
			// 	for (String k : aa.keySet()) {
			// 		String v = aa.get(k);
			// 		System.out.println(k + "  " + v);
			// 	}
			// 	System.out.println();
			// }

			// print in and out 
			// for (int p : coloredMap.keySet()) {
			// 	System.out.println(p);
			// 	ColoredNode n = coloredMap.get(p);
			// 	System.out.println(n.in + "  " + n.out);
			// 	System.out.println();
			// }

			// for (Integer c : callPosition.keySet()) {
			// 	for (Integer q : callPosition.get(c)) {
			// 		System.out.print(q + "   ");
			// 	}
			// 	System.out.println();
 		// 	}

			// for (Integer p : map.keySet()) {
			// 	HashMap<String, VarNode> m = map.get(p);
			// 	System.out.println(p);
			// 	for (String k : m.keySet()) {
			// 		VarNode node = m.get(k);
			// 		System.out.println(k + " " + node.startPoint + " " + node.endPoint);
			// 	}
			// 	System.out.println();
			// }

			// printVMT(vp);
			// printBody(vp);

 		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static VaporProgram parseVapor(InputStream in, PrintStream err)
		throws IOException {
		Op[] ops = {
			Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
			Op.PrintIntS, Op.HeapAllocZ, Op.Error,
		};
		boolean allowLocals = true;
		String[] registers = null;
		boolean allowStack = false;

		VaporProgram program;
		try {
			program = VaporParser.run(new InputStreamReader(in), 1, 1,
			                          java.util.Arrays.asList(ops),
			                          allowLocals, registers, allowStack);
		}
		catch (ProblemException ex) {
			err.println(ex.getMessage());
			return null;
		}
		return program;
	}
}






