Homework 2

I write two visitors, visitor 1 and visitor2 in this homework. Also, the main class is in the Typecheck.java and MyType.java is the class that I put my self-defined type classes.

Visitor1 is used to iterate the system.in and to figure out whether there are identical ids. If there are identical ids, just throw no-same-id exception. In the meantime, the class information is stored in the context and the fields and function signature of the class are stored in the classMap. So if a class is defined as follows
Class A {
	int a;
	boolean b;
	int fun1(int a, int v) {};
}
the context will contain {A -> classType}
the classMap will contain {A -> HashMap<>()}, which can be further derived as {a -> int, b -> boolean, fun1 -> int, int => int}
To make things easier, the ids of arguments in one function are not checked whether identical, and are not stored in the classMap.

In the second visitor, i.e. visitor2, I checked the contents of the methods and the main class. For each class, first I checked the according classMap and put the sub-environment in the current context, and then type-check the functions. Also, I had to check the argument ids and put them into the current environment. Then for each statement in the function, I checked if they are type-checked. For example, to the pattern “(expression1) + (expression2), I checked whether types of expression1 and type of expression2 are int, in order to achieve this, each of my MyType has to implement an abstract function, toType(), to indicate which type it is. Lastly, in the function body, there may exist this or classA, for instance, 

int p;
p = this.getMoney();

or 

List p;
Int a;
a = p.getSize();

In this situation, I have to put the String “this” and ClassName, ie, “List” into the current context and to indicate that these two string are represented a class.