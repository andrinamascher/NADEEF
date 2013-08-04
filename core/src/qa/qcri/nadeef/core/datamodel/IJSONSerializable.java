package qa.qcri.nadeef.core.datamodel;

import org.json.simple.JSONObject;

public interface IJSONSerializable<A> {
	public A fromJSON(JSONObject jsonObject);
	public JSONObject toJSON();
}