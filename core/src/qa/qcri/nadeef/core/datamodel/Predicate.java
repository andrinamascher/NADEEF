package qa.qcri.nadeef.core.datamodel;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.base.Preconditions;

public class Predicate implements IJSONSerializable<Predicate>{
	
	static class Validator{
		private Comparable comparable1;
		private Comparable comparable2;
		private Operation operation;
		
		public Validator(Comparable comparable1, Comparable comparable2, Operation operation){
			this.comparable1 = comparable1;
			this.comparable2 = comparable2;
			this.operation = operation;
		}
		
		@SuppressWarnings("unchecked")
		public boolean isValid(){
			boolean retValue = false;
			int compareResult = comparable1.compareTo(comparable2);
			switch (operation){
				case EQ:
					retValue = (compareResult == 0) ? true :false;
					break;
				case GT:
					retValue = (compareResult> 0) ? true : false;
					break;
				case GTE:
					retValue = (compareResult >= 0) ? true : false;
					break;
				case NEQ:
					retValue = (compareResult != 0) ? true : false;
					break;
				case LT:
					retValue = (compareResult < 0) ? true : false;
					break;
				case LTE:
					retValue = (compareResult <= 0) ? true : false;
					break;
				default:
					throw new IllegalArgumentException("unsupported operation: " + operation);
			}
			return retValue;
		}
		
	}
	
	private Operation op;
	private String left;
	private String right;
	private boolean isSingle;
	private boolean isRightConstant;
	
	private static Map<String, Operation> opMap = new HashMap<String, Operation>();
	
	static{
		opMap.put(">", Operation.GT);
		opMap.put(">=", Operation.GTE);
		opMap.put("=", Operation.EQ);
		opMap.put("<=", Operation.LTE);
		opMap.put("<", Operation.LT);
		opMap.put("!=", Operation.NEQ);
	}
	
	
	public static boolean checkIfValid(Predicate predicate, Tuple tuple){
		Preconditions.checkArgument(predicate.isSingle() == true);
		Cell leftHandCell = tuple.getCell(predicate.getLeft());
		Comparable leftHandSideComparable = null;
		Comparable rightHandSideComparable = null;
		Column leftSideColumn = leftHandCell.getColumn();
		switch (leftSideColumn.getType()){
			case STRING:
				leftHandSideComparable = leftHandCell.getValue().toString();
				break;
			case INTEGER:
				leftHandSideComparable = Integer.parseInt(leftHandCell.getValue().toString());
				break;
			case DOUBLE:
				leftHandSideComparable = Double.parseDouble(leftHandCell.getValue().toString());
				break;
			default:
				throw new IllegalArgumentException("unsupported data type: " + leftSideColumn.getType());
		}
		if (!predicate.isRightConstant()){
			Cell rightHandCell = tuple.getCell(predicate.getRight());

			Column rightSideColumn = rightHandCell.getColumn();
			switch (rightSideColumn.getType()){
				case STRING:
					rightHandSideComparable = rightHandCell.getValue().toString();
					break;
				case INTEGER:
					rightHandSideComparable = Integer.parseInt(rightHandCell.getValue().toString());
					break;
				case DOUBLE:
					rightHandSideComparable = Double.parseDouble(rightHandCell.getValue().toString());
					break;
				default:
					throw new IllegalArgumentException("unsupported data type: " + rightSideColumn.getType());
			}
		}else{
			switch (leftSideColumn.getType()){
				case STRING:
					rightHandSideComparable = predicate.getRight();
					break;
				case INTEGER:
					rightHandSideComparable = Integer.parseInt(predicate.getRight());
					break;
				case DOUBLE:
					rightHandSideComparable = Double.parseDouble(predicate.getRight());
					break;
				default:
					throw new IllegalArgumentException("unsupported data type: " + leftSideColumn.getType());
			}
		}
		Validator validator = new Validator(leftHandSideComparable,rightHandSideComparable,predicate.getOp());
		boolean retValue =  validator.isValid();
		return retValue;
	}
	
	public static boolean checkIfPairValid(Predicate predicate, Tuple tuple1, Tuple tuple2){
		Preconditions.checkArgument(predicate.isSingle() == false);
		Cell leftHandCell = tuple1.getCell(predicate.getLeft());
		Cell rightHandCell = tuple2.getCell(predicate.getRight());
		Column leftHandColumn = leftHandCell.getColumn();
		Column rightHandColumn = rightHandCell.getColumn();
		Preconditions.checkArgument(leftHandColumn.getType() == rightHandColumn.getType());
		Comparable leftHandSideComparable = null;
		Comparable rightHandSideComparable = null;
		switch (leftHandColumn.getType()){
			case STRING:
				leftHandSideComparable = leftHandCell.getValue().toString();
				rightHandSideComparable = rightHandCell.getValue().toString();
				break;
			case INTEGER:
				leftHandSideComparable = Integer.parseInt(leftHandCell.getValue().toString());
				rightHandSideComparable = Integer.parseInt(rightHandCell.getValue().toString());
				break;
			case DOUBLE:
				leftHandSideComparable = Double.parseDouble(leftHandCell.getValue().toString());
				rightHandSideComparable = Double.parseDouble(rightHandCell.getValue().toString());
				break;
		}
		Validator validator = new Validator(leftHandSideComparable,rightHandSideComparable,predicate.getOp());
		return validator.isValid();
	}
	
	public Operation getOp() {
		return op;
	}
	public void setOp(Operation op) {
		this.op = op;
	}
	public String getLeft() {
		return left;
	}
	public void setLeft(String left) {
		this.left = left;
	}
	public String getRight() {
		return right;
	}
	public void setRight(String right) {
		this.right = right;
	}
	public boolean isSingle() {
		return isSingle;
	}
	public void setSingle(boolean isSingle) {
		this.isSingle = isSingle;
	}
	public boolean isRightConstant() {
		return isRightConstant;
	}
	public void setRightConstant(boolean isRightConstant) {
		this.isRightConstant = isRightConstant;
	}
	
	public static Operation getMappedOperation(String operationName){
		return opMap.get(operationName);
	}

	@Override
	public Predicate fromJSON(JSONObject jsonObject)  {	
		String op = jsonObject.get("operation").toString();
		String left = jsonObject.get("left").toString();
		String right = jsonObject.get("right").toString();
		Boolean isSingle = Boolean.parseBoolean(jsonObject.get("isSingle").toString());
		Boolean isRightConstant = Boolean.parseBoolean(jsonObject.get("isRightConstant").toString());
		Operation operation = Operation.valueOf(op);
		this.setOp(operation);
		this.setLeft(left);
		this.setRight(right);
		this.setSingle(isSingle);
		this.setRightConstant(isRightConstant);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("operation", op.toString());
		jsonObject.put("left", left);
		jsonObject.put("right", right);
		jsonObject.put("isSingle", isSingle);
		jsonObject.put("isRightConstant", isRightConstant);
		return jsonObject;
	}
}
