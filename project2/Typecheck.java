import syntaxtree.*;
import visitor.*;
import java.util.*;

public class Typecheck {
	public static void main(String[] args) {
		try {
	        Goal g = new MiniJavaParser(System.in).Goal();
	        HashMap<String, MyType> context = new HashMap<>();
	        HashMap<String, HashMap<String, MyType>> classMap = new HashMap<>();
	        Visitor1 v1 = new Visitor1(context, classMap);
	        g.accept(v1, context);


            // for (String p : context.keySet()) {
            //   System.out.print(p + "  ");
            //   System.out.println(context.get(p).toString());
            // }
            // System.out.println();

            // for (String p : classMap.keySet()) {
            //   System.out.println(p);
            //   HashMap<String, MyType> m = classMap.get(p);
            //   for (String s : m.keySet()) {
            //     System.out.println(s);
            //     System.out.println(m.get(s).toString());
            //   }
            // }

	        Visitor2 v2 = new Visitor2(context, classMap);
	        g.accept(v2, context);

            System.out.println("Program type checked successfully");
		} catch (Exception e) {
        //e.printStackTrace();
    		System.out.println("Type error");
    	}
	}
}

