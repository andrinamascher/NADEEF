import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Test {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {
//		String predicatesInJSON = "[{\"operation\":\"EQ\",\"isRightConstant\":false,\"left\":\"C\",\"isSingle\":false,\"right\":\"C\"}]";
//		JSONParser parser=new JSONParser();
//		JSONArray array = (JSONArray)parser.parse(predicatesInJSON);
//		for (Object obj : array){
//			System.out.println(obj.toString());
//		}
		String str = "\"";
		System.out.println(str.replaceAll("\"", "\\\\\""));
	}

}
