import syntaxtree.*;
import visitor.*;
import java.util.*;

public class Visitor2 extends GJDepthFirst<MyType, Map<String, MyType>> { 

	private HashMap<String, MyType> context;
	private HashMap<String, HashMap<String, MyType>> classMap;

	public Visitor2(HashMap<String, MyType> context, HashMap<String, HashMap<String, MyType>> classMap) {
      this.context = context;
      this.classMap = classMap;
    }

    /***********************
    if the return value is StringType, then 
    look up in the context to get the real type of the returned value
    **********************/

    public MyType visit(Goal g, Map<String, MyType> map) {
        // type check for MainClass
        g.f0.accept(this, map);
        g.f1.accept(this, map);
        return null;
    }

    public MyType visit(MainClass mc, Map<String, MyType> map) {
      // not deal with f1 and f11 at this time
      // f14 -> ( VarDeclaration() )*
      // f15 -> ( Statement() )*
      Map<String, MyType> m = new HashMap<>(map);
      mc.f14.accept(this, m);
      mc.f15.accept(this, m);
      return null;
    }

     public MyType visit(NodeListOptional nlo, Map<String, MyType> map) {
      List<MyType> list = new ArrayList<>();
      List<String> checkId = new ArrayList<>();
      for (Enumeration<Node> e = nlo.elements(); e.hasMoreElements(); ) {
        MyType p = e.nextElement().accept(this, map);
        if (p != null) list.add(p);
        if (p instanceof MyParameterType || p instanceof StringType || p instanceof ClassType) {
          checkId.add(p.toId());
        }
      }
      // need to judge the identifiers in a list are pairwise distinct
      if (!distinct(checkId)) throw new RuntimeException("same ids in a list");
      return MyType.fromList(list);
    }

    /******************** Statement ********************/

    public MyType visit(Statement stm, Map<String, MyType> map) {
      return stm.f0.accept(this, map);  
    }

    public MyType visit(PrintStatement ps, Map<String, MyType> map) {
      // f2 -> Expression()
      MyType t = ps.f2.accept(this, map);
      // if the type of t is not MyIntegerType, then throw error
      if (t.toType().equals("String")) {
        if (map.containsKey(t.toString())) {
          return null;
        } else {
          throw new RuntimeException("The variable " + t.toString() + " is not in the context");
        }
      }
      if (t.toType().equals("int")) {
        return null;
      } else {
        throw new RuntimeException("The PrintStatement should print integer type");
      }
    }

    public MyType visit(IfStatement is, Map<String, MyType> map) {
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
      MyType f = is.f2.accept(this, map);
      if (f != null) {
        if (f.toType().equals("boolean") || 
          (f.toType().equals("String") && lookup(f, map).toType().equals("boolean"))) {
          is.f4.accept(this, map);
          is.f6.accept(this, map);
        } else {
          throw new RuntimeException("the expression in if(*) should be boolean");
        }
      } else {
        throw new RuntimeException("the expression in if(*) should be boolean");
      }
      return null;
    }

    public MyType visit(AssignmentStatement as, Map<String, MyType> map) {
      /**
       * Grammar production:
       * f0 -> Identifier()
       * f2 -> Expression()
       */
      MyType f0 = as.f0.accept(this, map);
      MyType f2 = as.f2.accept(this, map);
      MyType a = lookup(f0, map);
      if (a.toType().equals(f2.toType())) {
        return null;
      } else {
        throw new RuntimeException("The two arguments of assignment statement are not type check");
      }
    }

    public MyType visit(WhileStatement ws, Map<String, MyType> map) {
      /**
       * f2 -> Expression()
       * f4 -> Statement()
       */
      MyType f2 = ws.f2.accept(this, map);
      MyType f4 = ws.f4.accept(this, map);
      if (f2.toType().equals("boolean")) return null;
      else {
        throw new RuntimeException("the expression in while(*) should be boolean");
      }
    }

    public MyType visit(ArrayAssignmentStatement aas, Map<String, MyType> map) {
      /**
       * f0 -> Identifier()
       * f1 -> "["
       * f2 -> Expression()
       * f3 -> "]"
       * f4 -> "="
       * f5 -> Expression()
       */
      MyType f0 = aas.f0.accept(this, map);
      MyType f2 = aas.f2.accept(this, map);
      MyType f5 = aas.f5.accept(this, map);
      if (lookup(f0, map).toType().equals("int[]")
        && f2.toType().equals("int")
        && f5.toType().equals("int")) {
        return null;
      } else {
        throw new RuntimeException("ArrayAssignmentStatement is not type check be");
      }
    }

    public MyType visit(Block b, Map<String, MyType> map) {
      /**     
      * f0 -> "{"
      * f1 -> ( Statement() )*
      * f2 -> "}"
      */
      b.f1.accept(this, map);
      return null;
    }
    
    

    /***************************************************/

    /******************** Expression ******************/

    // in the expression, if the returned type is identifier, then look up in the table 
    // to find whether id is in the table; if true, retrieve the MyType according to id
    // if not, throw id-not-in-context exception.
    // if the returned type is not identifier, just return  
    public MyType visit(Expression e, Map<String, MyType> map) {
      MyType p = e.f0.accept(this, map);
      if (p.toType().equals("String")) {
        MyType t = lookup(p, map);
        return t;
      }
      return p;
    }

    public MyType visit(PrimaryExpression pe, Map<String, MyType> map) {
      return pe.f0.accept(this, map);
    }

    public MyType visit(CompareExpression ce, Map<String, MyType> map) {
      MyType f0 = ce.f0.accept(this, map);
      MyType f2 = ce.f2.accept(this, map);
      return primaryHelper(f0, f2, "int", "int", new MyBooleanType(), map);
    }

    public MyType visit(AndExpression ae, Map<String, MyType> map) {
      MyType f0 = ae.f0.accept(this, map);
      MyType f2 = ae.f2.accept(this, map);
      return primaryHelper(f0, f2, "boolean", "boolean", new MyBooleanType(), map);
    }

    public MyType visit(PlusExpression pe, Map<String, MyType> map) {
      MyType f0 = pe.f0.accept(this, map);
      MyType f2 = pe.f2.accept(this, map);
      return primaryHelper(f0, f2, "int", "int", new MyIntegerType(), map);
    }

    public MyType visit(MinusExpression pe, Map<String, MyType> map) {
      MyType f0 = pe.f0.accept(this, map);
      MyType f2 = pe.f2.accept(this, map);
      return primaryHelper(f0, f2, "int", "int", new MyIntegerType(), map);
    }

    public MyType visit(TimesExpression pe, Map<String, MyType> map) {
      MyType f0 = pe.f0.accept(this, map);
      MyType f2 = pe.f2.accept(this, map);
      return primaryHelper(f0, f2, "int", "int", new MyIntegerType(), map);
    }

    public MyType visit(ArrayLookup pe, Map<String, MyType> map) {
      MyType f0 = pe.f0.accept(this, map);
      MyType f2 = pe.f2.accept(this, map);
      return primaryHelper(f0, f2, "int[]", "int", new MyIntegerType(), map);
    }

    public MyType visit(ArrayLength pe, Map<String, MyType> map) {
      MyType f0 = pe.f0.accept(this, map);
      if (f0 != null) {
        if (f0.toType().equals("int[]")) {
          return new MyIntegerType();
        } else if (f0.toType().equals("String") && lookup(f0, map).toType().equals("int[]")) {
          return new MyIntegerType();
        }
      }
      throw new RuntimeException("The two operators in the PrimaryExpression are not type check");
    }

    // this helper funcion is used for AndEpxpression, CompareExpression, PlusExpression, MinusExpression, TimesExpression, ArrayLookUp
    private MyType primaryHelper(MyType f0, MyType f2, String desiredType0, String desiredType2, MyType returnType, Map<String, MyType> map) {
      if (f0 == null || f2 == null) {
        throw new RuntimeException("The two operators in the PrimaryExpression are not type check");
      }
      if ((f0.toType().equals(desiredType0) || (f0.toType().equals("String") && lookup(f0, map).toType().equals(desiredType0)))
        && (f2.toType().equals(desiredType2) || (f2.toType().equals("String") && lookup(f2, map).toType().equals(desiredType2)))) {
        return returnType;
      } else {
        throw new RuntimeException("The two operators in the PrimaryExpression are not type check");
      }
    }

    public MyType visit(MessageSend ms, Map<String, MyType> map) {
      // f0 -> PrimaryExpression()
      // f1 -> "."
      // f2 -> Identifier()
      // f3 -> "("
      // f4 -> ( ExpressionList() )?  , NodeOptional
      // f5 -> ")"
      MyType m = ms.f0.accept(this, map);
      // first situation ***********************
      // if the f0 is new Identifier(), ie. AllocationExpression

      // f0 should be new A() or the current Class type
      // so check both cases
      Map<String, MyType> functionsInClass = null;
      if (classMap.containsKey(m.toString())) {
          functionsInClass = classMap.get(m.toString());
      } else {
        if (map.getOrDefault(m.toString(), null) != null) {
          functionsInClass = classMap.get(map.get(m.toString()).toString());
        }
      }
      if (functionsInClass == null) {
        throw new RuntimeException("Not in the context");
      }
      MyType id = ms.f2.accept(this, map);
      MyType func = null;
      // is the funtion defined in the Class 
      if (!functionsInClass.containsKey(id.toString())) {
        throw new RuntimeException("The function " + id.toString() + " is not defined in Class " + m.toString());
      } else {
        // is the function name or field name
        func = functionsInClass.get(id.toString());
        if (func instanceof MethodType) {
        } else {
          throw new RuntimeException("The function " + id.toString() + " is not defined in Class " + m.toString());
        }
      }
      // need to attention that list will be checked distinct
      // so Expression should express something else 
      // maybe not, just revise the NodeListOptional method

      //f4 -> ( ExpressionList() )? 
      MethodType mt = (MethodType) func;
      MyType parameterList = ms.f4.accept(this, map);
      MyType actualParameterList = mt.mlt;
      // judge that if the parameter matches

      if (actualParameterList == null) {
        if (parameterList == null) {
          // type check
        } else {
          throw new RuntimeException("the arguments are not correct");  
        }
      } else {
        if (actualParameterList.equals(parameterList)) {
          // type check
        } else {
          throw new RuntimeException("the arguments are not correct");
        }
      }
      return mt.result;
    }
    /***********************************************************

    /********************* primary Expression ******************/
    public MyType visit(AllocationExpression ae, Map<String, MyType> map) {
      /**
       * f0 -> "new"
       * f1 -> Identifier()
       */

      MyType t = ae.f1.accept(this, map);
      if (!map.containsKey(t.toString())) {
        if (!classMap.containsKey(t.toString())) {
          throw new RuntimeException("Class " + t.toString() + " is not defined"); 
        } else {
          return new ClassType(t.toString());
        }
      }
      return t;
    }

    public MyType visit(TrueLiteral tl, Map<String, MyType> map) {
      return new MyBooleanType();
    }

    public MyType visit(FalseLiteral tl, Map<String, MyType> map) {
      return new MyBooleanType();
    }

    public MyType visit(IntegerLiteral il, Map<String, MyType> map) {
      return new MyIntegerType();
    }

    public MyType visit(NotExpression ne, Map<String, MyType> map) {
      MyType f1 = ne.f1.accept(this, map);
      if (f1.toType().equals("boolean")) {
        return new MyBooleanType();
      } else {
        throw new RuntimeException("The operator in NotExpression should be boolean");
      }
    }

    public MyType visit(Identifier id, Map<String, MyType> map) {
      StringType st = new StringType(id.f0.toString());
      return st;
    }

    public MyType visit(ArrayAllocationExpression aae, Map<String, MyType> map) {
      /**
       * Grammar production:
       * f0 -> "new"
       * f1 -> "int"
       * f2 -> "["
       * f3 -> Expression()
       * f4 -> "]"
       */
      MyType f3 = aae.f3.accept(this, map);
      if (f3.toType().equals("int")) {
        return new MyArrayType();
      } else {
        throw new RuntimeException("The ArrayAllocationExpression is not type check");
      }
    }

    public MyType visit(BracketExpression be, Map<String, MyType> map) {
      /**
       * f0 -> "("
       * f1 -> Expression()
       * f2 -> ")"
       */
      return be.f1.accept(this, map);
    }

    public MyType visit(ThisExpression te, Map<String, MyType> map) {
      return new StringType("this");
    }

    /***********************************************************/

    public MyType visit(ExpressionList el, Map<String, MyType> map) {
      List<MyType> list = new LinkedList<>();
      MyType a = el.f0.accept(this, map);
      // when there is no arguments
      if (a == null) return MyType.fromList(list);
      list.add(a);
      MyType b = el.f1.accept(this, map);
      if (b != null) list.add(b);
      return MyType.fromList(list);
    }

    public MyType visit(ExpressionRest er, Map<String, MyType> map) {
      return er.f1.accept(this, map);
    }

    public MyType visit(TypeDeclaration td, Map<String, MyType> map) {
      return td.f0.accept(this, map);
    }

    public MyType visit(ClassDeclaration cd, Map<String, MyType> map) {
      // first load the information of the class into the context;
      // for each method declaration, add the arguments into its own sub enviroment  
      /**
       * f0 -> "class"
       * f1 -> Identifier()
       * f2 -> "{"
       * f3 -> ( VarDeclaration() )*
       * f4 -> ( MethodDeclaration() )*
       * f5 -> "}"
       */
      String id = cd.f1.accept(this, map).toString();
      Map<String, MyType> m = classMap.get(id);
      m.put(id, new ClassType(id));
      m.put("this", new ClassType(id));
      cd.f4.accept(this, m);
      return null;
    }

    public MyType visit(ClassExtendsDeclaration ced, Map<String, MyType> map) {
      // first load the information of the class into the context;
      // for each method declaration, add the arguments into its own sub enviroment  
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
      String id = ced.f1.accept(this, map).toString();
      Map<String, MyType> m = classMap.get(id);
      m.put(id, new StringType(id));
      m.put("this", new StringType(id));
      ced.f6.accept(this, m);
      return null;
    }


    public MyType visit(VarDeclaration vd, Map<String, MyType> map) {
      // f0 -> Type()
      // f1 -> Identifier()
        // only varDeclarations in mainclass should be checked in the second round
        MyType id = vd.f1.accept(this, map);
        MyType mt = vd.f0.accept(this, map);
        map.put(id.toString(), mt);
        return id;
    }

    public MyType visit(MethodDeclaration md, Map<String, MyType> map) {
      /**
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
       */      // put the argument in the formalParameterList into context
      md.f4.accept(this, map);
      md.f7.accept(this, map);
      md.f8.accept(this, map);
      MyType f10 = md.f10.accept(this, map);
      if (!f10.toType().equals(md.f1.accept(this, map).toType())) {
        throw new RuntimeException("The method " + md.f2.accept(this,map).toString() + " returns wrong type");
      }
      return null;
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

      // f0 -> FormalParameter()
      // f1 -> ( FormalParameterRest() )*

      List<MyType> list = new LinkedList<>();
      HashMap<String, MyType> parameterMap = new HashMap<>();

      fpl.f0.accept(this, parameterMap);
      fpl.f1.accept(this, parameterMap);

      for (String p : parameterMap.keySet()) {
        map.put(p, parameterMap.get(p));
      }
      // if a in b: to avoid same ids
      return null;
    }

    // *******************
    // 

    public MyType visit(FormalParameter fp, Map<String, MyType> map) {
      MyType t = fp.f0.accept(this, map);
      // when the type is a String, ie. a identifier
      // it should be a Class, or current class
      // if (t.toType().equals("String"))
      MyType i = fp.f1.accept(this, map);
      // System.out.println(t.toString() + "   " + i.toString());
      if (map.containsKey(i.toString())) {
        throw new RuntimeException("Same id in the FormalParameter");
      }
      map.put(i.toString(), t);
      return new MyParameterType(t, i);
    }

    public MyType visit(FormalParameterRest fpr, Map<String, MyType> map) {
      return fpr.f1.accept(this, map);
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

    private MyType lookup(MyType p, Map<String, MyType> map) {
      if (p.toType() != "String" || !map.containsKey(p.toString())) {
        throw new RuntimeException("variable " + p.toString() + " is not in the context" );
      } else {
        return map.get(p.toString());
      }
    }
}















