import syntaxtree.*;
import visitor.*;
import java.util.*;

public class Visitor1 extends GJDepthFirst<MyType, Map<String, MyType>> {
	
	private HashMap<String, MyType> context;
	private HashMap<String, HashMap<String, MyType>> classMap;

	public Visitor1(HashMap<String, MyType> context, HashMap<String, HashMap<String, MyType>> classMap) {
      this.context = context;
      this.classMap = classMap;
    }

    public MyType visit(Goal g, Map<String, MyType> map) {
      g.f1.accept(this, map);
      return null;
    }

    public MyType visit(NodeListOptional nlo, Map<String, MyType> map) {
      List<MyType> list = new ArrayList<>();
      List<String> checkId = new ArrayList<>();
      List<MyType> checkCircle = new ArrayList<>();
      for (Enumeration<Node> e = nlo.elements(); e.hasMoreElements(); ) {
        MyType p = e.nextElement().accept(this, map);
        list.add(p);
        if (p instanceof MyParameterType || p instanceof StringType || p instanceof ClassType
          || p instanceof ExtendsClassType) {
         	checkId.add(p.toId());
        }
        if (p instanceof ClassType || p instanceof ExtendsClassType) {
          checkCircle.add(p);
        }
      }
      // need to judge the identifiers in a list are pairwise distinct
      if (!distinct(checkId)) throw new RuntimeException("same ids in a list");
      if (existCircle(checkCircle)) throw new RuntimeException("there is circle in the class and extend class");
      return MyType.fromList(list);
    }

    private boolean existCircle(List<MyType> list) {
       LinkedList<String> zero = new LinkedList<>();
       HashMap<String, Integer> indegree = new HashMap<>();
       for (MyType p : list) {
          if (p instanceof ClassType) {
            zero.add(p.toId());
          } else {
            String father = ((ExtendsClassType) p).getFather();
            String son = p.toId();
            indegree.put(son, indegree.getOrDefault(son, 0) + 1);
          }
       }
       while (!zero.isEmpty()) {
        String p = zero.remove(0);
        for (MyType q : list) {
          if (q instanceof ExtendsClassType) {
            String father = ((ExtendsClassType) q).getFather();
            if (father.equals(p)) {
              int degree = indegree.get(q.toId()) - 1;
              if (degree == 0) {
                zero.add(q.toId());
                indegree.remove(q.toId());
              } else {
                indegree.put(q.toId(), degree);
              }
            }
          }
        }
       }
       return indegree.size() != 0;
    }


    public MyType visit(TypeDeclaration td, Map<String, MyType> map) {
      return td.f0.choice.accept(this, map);
    }

    public MyType visit(ClassDeclaration cd, Map<String, MyType> map) {
      MyType result = cd.f1.accept(this, map);
      String s = result.toString();
	    // f1 : Identifier()
	    // f3 : (VarDeclaration() )*
	    // f4 : (MethodDeclaration() )*
	    // String s = cd.f1.accept(this, map).toString();
	    map.put(s, new ClassType("Class " + s));
	    HashMap<String, MyType> m = new HashMap<>();
	    // the member in this list should be Indentifier to check
	    // whether there are two identical ids in the VarDeclaration
	    cd.f3.accept(this, m);
	    cd.f4.accept(this, m);
	    classMap.put(s, new HashMap<>(m));
      
      return new ClassType(s);
    }

    public MyType visit(ClassExtendsDeclaration ced, Map<String, MyType> map) {
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
      MyType result1 = ced.f1.accept(this, map);
      MyType result3 = ced.f3.accept(this, map);
      String s = result1.toString();
      map.put(s, new ClassType("Class " + s));
      HashMap<String, MyType> m = new HashMap<>();
      ced.f5.accept(this, m);
      ced.f6.accept(this, m);

      HashMap<String, MyType> basicClass = classMap.get(result3.toString());
      classMap.put(s, new HashMap<>(basicClass));
      HashMap<String, MyType> k = classMap.get(s);

      for (String q : m.keySet()) {
        k.put(q, m.get(q));
      }

      return new ExtendsClassType(new ClassType(result3.toString()), new ClassType(s));
    }

    public MyType visit(VarDeclaration vd, Map<String, MyType> map) {
      // f0 -> Type()
      // f1 -> Identifier()
        MyType id = vd.f1.accept(this, map);
        MyType mt = vd.f0.accept(this, map);
        map.put(id.toString(), mt);
        return id;
    }

    public MyType visit(MethodDeclaration md, Map<String, MyType> map) {
        // f1 -> Type()
        // f2 -> Identifier()
        // f4 -> ( FormalParameterList() )?  type: Nodeoptional (0 or 1)
        MyType result = md.f1.accept(this, map);

        // the NodeOptional
        MyType mm = md.f2.accept(this, map);
        // l is myList type
        MyType l = md.f4.accept(this, map);
        map.put(mm.toString(), new MethodType(l, result));
        return mm;
     }

    public MyType visit(NodeOptional no, Map<String, MyType> map) {
      if (no.node == null) return null;
      return no.node.accept(this, map);
    }

    public MyType visit(FormalParameterList fpl, Map<String, MyType> map) {
      // should check whether 
      // a is in the b to avoid same ids
      // because b is a NodeOptionList so all ids in b
      // are distinct 
      List<MyType> list = new LinkedList<>();

      // a : MyParameterType
      MyType a = fpl.f0.accept(this, map);
      if (a != null) list.add(a);
      // b : MyListType
      MyType b = fpl.f1.accept(this, map);
      if (b != null) list.add(b);
      MyType l = MyType.fromList(list);
      return l;
    }

    // not check the equal ids in the first round
    // so fp.f1 is not checked
    public MyType visit(FormalParameter fp, Map<String, MyType> map) {
      MyType t = fp.f0.accept(this, map);
      MyType i = fp.f1.accept(this, map);
      return new MyParameterType(t, i);
    }

    public MyType visit(FormalParameterRest fpr, Map<String, MyType> map) {
      return fpr.f1.accept(this, map);
    }

    public MyType visit(Identifier id, Map<String, MyType> map) {
      StringType st = new StringType(id.f0.toString());
      return st;
    }

    public MyType visit(Type t, Map<String, MyType> map) {
      switch (t.f0.which) {
        case 0: 
          return new MyArrayType();
        case 1:
          return new MyBooleanType();
        case 2: 
          return new MyIntegerType();
        case 3:
          return new ClassType(t.f0.accept(this, map).toString());
        default:
          return null;
      }
    }

    private boolean distinct(List<String> list) {
      HashSet<String> set = new HashSet<>();
      for (String l : list) {
        if (set.contains(l)) return false;
        set.add(l);
      }
      return true;
    }
}


























