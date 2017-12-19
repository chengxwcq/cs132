// should use a list not a map to store the vitual method table
// because the order of the methods matters
// when using the method call, the order of the method k * 4 or (k + 1) * 4
// is used to get the right method


// the numbers in the if<num>_else, while<num>, null<num> are each consecutive between functions,
// use three global variables to store them

/*
	first iteration: 
	
		use a HashMap<String, LinkedList<LinkedList<String>>> to store 
		the class information. the length of each linkedlist is 2. the 
		first thing in the LinkedList should be Virtual Method Table, 
		which is also a linkedlist storing the methods in the class; the 
		second thing is a linkedlist contained the fields of the class

		it seems mytype is no needed here because the file has
		already type checked

		get the vitual method table(using list to store) and 
		fields of each class, store them in a list, because 
		we need to use the index to get the field.
		
		No need to store the arguments of the method now because
		we only need them in the second iteration. 

	second iteration:

		Use while_no, if_no and ... to record the number of goto label

		when encourter an id in the method, first check whether 
		it's defined in the method or in the arguments of the 
		method. If not, check whether it's in the class field.
		So the argument and the ids defined in method can be stored
		into the map because the order of them doesn't matter.
*/
import syntaxtree.*;
import visitor.*;
import java.util.*;

public class J2V {
	public static void main(String[] args) {
		try {
			HashMap<String, LinkedList<LinkedList<String>>> map = new HashMap<>();
			HashMap<String, LinkedList<LinkedList<String>>> vaporMap = new HashMap<>();
			Goal g = new MiniJavaParser(System.in).Goal();
			FirstVisitor p = new FirstVisitor(map, vaporMap);
		    g.accept(p, new LinkedList<>());

		    // output the virtual method Table
		    for (String key : vaporMap.keySet()) {
		    	System.out.println("const vmt_" + key);
		    	LinkedList<String> vmd = vaporMap.get(key).get(0);
		    	for (String method : vmd) {
		    		System.out.println("  :" + method);
		    	}
		    	System.out.println();
		    }

		    StringBuilder vapor = new StringBuilder();
		    SecondVisitor q = new SecondVisitor(map, vapor);
		    g.accept(q, new HashMap<>());
		    System.out.println(vapor.toString());

		} catch (Exception e) {
			e.printStackTrace();
    		System.out.println("error");
    	}
		
	}
}


