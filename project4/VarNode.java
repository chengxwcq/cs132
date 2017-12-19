// this class is used to record the liveness range of 
// each variable occured in the program

class VarNode {
	String name;
	int startPoint;
	int endPoint; 
	
	VarNode(String name, int startPoint, int endPoint) {
		this.name = name;
		this.startPoint = startPoint;
		this.endPoint = endPoint;
	}

	VarNode() {

	}

	void setStartPoint(int startPoint) {
		this.startPoint = startPoint;
	}

	void setEndPoint(int endPoint) {
		this.endPoint = endPoint;
	}

	void setName(String name) {
		this.name = name;
	}
}