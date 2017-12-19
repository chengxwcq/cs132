import syntaxtree.*;
import visitor.*;
import java.util.*;

abstract class MyType{
  public abstract String toString();
  public abstract String toType();
  public abstract boolean equals(Object o);

  public String toId() {
    //System.out.println("WARNING: each class should implement their own toId method!");
    return null;
  }

  public boolean checkSameId(MyType a) {
    //System.out.println("checkSameId() function should not be used in MyType");
    return true;
  }
  
  public int length() { return 0; }
  // for test ************

  public int hashCode() { return 1; }

  // ****************
  public static MyType fromList(List<MyType> list) {
    int size = list.size();
    if (size == 0) return null;
    else if (size == 1) return new MyListType(list.get(0), null);
    else return new MyListType(list.get(0), MyType.fromList(list.subList(1, list.size())));
  }
}

class MyListType extends MyType{
  MyType from; 
  MyType to;

  public MyListType(MyListType p) {
    from = p.from;
    to = p.to;
  }

  public MyListType(MyType from, MyType to) {
    this.from = from;
    this.to = to;
  } 

  public boolean checkSameId(MyType t) {
    return t.equals(from) && t.equals(to);
  }

  public String toType() {
    return "list";
  }

  public int length() {
    if (from == null) return 0;
    if (to != null) {
      return 1 + ((MyListType) to).length();
    } else {
      return 1;
    }
  }

  public String toString() {
    String f = "";
    if (from != null) {
      f += from.toString();
    } else {
      return "Nil";
    }
    if (to != null) {
      f += " " + to.toString();
    } else {
      return f;
    }
    return f;
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof MyListType) {
      MyListType p = (MyListType) o;
      if (this.from.equals(p.from)) {
        if (this.to == null) {
          return p.to == null;
        } else {
          return this.to.equals(p.to);
        }
      } else {
        return false;
      }
     // return this.from.equals(p.from) && this.to.equals(p.to);
    } else {
      return false;
    }
  }
}

class MyParameterType extends MyType {
  MyType type;
  StringType id;
  public MyParameterType(MyType type, MyType id) {
    if (id != null && id instanceof StringType) {
      this.type = type;
      this.id = (StringType) id;
    }
  }

  public String toString() {
    return type.toString();
  }

// MyParameterType contains id, so when check if the parameter type check,
// it should key the toType; 
  public boolean equals(Object o) {
    if (o != null && o instanceof MyParameterType) {
      MyParameterType p = (MyParameterType) o;
      return p.id.equals(this.id);
    } 
    if (o != null && o instanceof MyType) {
      return type.toType().equals(((MyType)o).toType());
    }
    else {
      return false;
    }
  }

  public String toId() {
    return id.toString();
  }

  public String toType() {
    return "parameter";
  }
}

class MyArrayType extends MyType{

  public String toString() {
    return "int[]";
  }

  public String toType() {
    return "int[]";
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof MyArrayType) {
      return true;
    } else {
      return false;
    }
  }
}

class MyIntegerType extends MyType {
  public String toString() {
    return "int";
  }

  public String toType() {
    return "int";
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof MyIntegerType) {
      return true;
    } else {
      return false;
    }
  }
}

class MyBooleanType extends MyType {
  public String toString() {
    return "boolean";
  }

  public String toType() {
    return "boolean";
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof MyBooleanType) {
      return true;
    } else {
      return false;
    }
  }
}

class StringType extends MyType {
  final String s;
  public StringType(String s) {
    this.s = s;
  }

  public String toString() {
    return s;
  }

  public String toId() {
    return s;
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof StringType) {
      return s.equals(((StringType)o).s);
    } else {
      return false;
    }
  }

  public String toType() {
    return "String";
  }
}

// because distinct funtion has to check the id of the class
// so ClassType has the field id

class ClassType extends MyType {

  String id = "";

  public ClassType(String id) { this.id = id; }

  public String toString() {
    return id;
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof ClassType) {
      return this.id.equals(((ClassType) o).id);
    } else {
      return false;
    }
  }

  public String toId() {
    return id;
  }

  public int hashCode() {
    return id.hashCode();
  }

  public String toType() {
    return id;
  }
}

class ExtendsClassType extends MyType {
  ClassType father;
  ClassType son;

  public ExtendsClassType(ClassType father, ClassType son) {
    this.father = father;
    this.son = son;
  }

  public String toString() {
    return father.toString() + " -> " + son.toString();
  }

  public String toType() {
    return "ExtendsClassType";
  }

  public String toId() {
    return son.toId();
  }

  public String getFather() {
    return father.toId();
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof ExtendsClassType) {
      ExtendsClassType p = (ExtendsClassType) o;
      return p.father.equals(this.father) && p.son.equals(this.son);
    } else {
      return false;
    }
  }
}

class MethodType extends MyType {
  MyListType mlt;
  MyType result;
  public MethodType(MyType mlt, MyType result) {
    if (mlt == null) {
      this.result = result;
      return;
    }
    if (mlt instanceof MyListType) {
      this.mlt = new MyListType((MyListType)mlt);
      this.result = result;  // maybe cause problem here
    } else {
      throw new RuntimeException("MethodType should be initialized with the MyListType argument");
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (mlt != null) sb.append(mlt.toString());
    sb.append(" -> ").append(result.toString());
    return sb.toString();
  }

  public String toType() {
    return "method";
  }

  public boolean equals(Object o) {
    if (o != null && o instanceof MethodType) {
      return true;
    } else {
      return false;
    }
  }
}

class SameIdException extends RuntimeException {
  public SameIdException (String msg) {
    super(msg);
  }
}





