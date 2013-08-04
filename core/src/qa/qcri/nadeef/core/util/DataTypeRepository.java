package qa.qcri.nadeef.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

import qa.qcri.nadeef.core.datamodel.DataType;

public class DataTypeRepository {

	private static Object lock = new Object();

	private static Map<String, Map<String, DataType>> dataTypeRepository;

	static {
		dataTypeRepository = new ConcurrentHashMap<String, Map<String, DataType>>();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, DataType> getTypeMapping(String schemaName, String tableName) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("schemaName", schemaName);
		jsonObject.put("tableName", tableName);
		if (!dataTypeRepository.containsKey(jsonObject.toString())) {
			synchronized (lock) {
				if (!dataTypeRepository.containsKey(jsonObject.toString())){
					dataTypeRepository.put(jsonObject.toString(), new ConcurrentHashMap<String, DataType>());
				}
			}
		}
		return dataTypeRepository.get(jsonObject.toString());
	}
	@SuppressWarnings("unchecked")
	public static Map<String, DataType> getTypeMapping(String tableName) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("schemaName", "public");
		jsonObject.put("tableName", tableName);
		if (!dataTypeRepository.containsKey(jsonObject.toString())) {
			synchronized (lock) {
				if (!dataTypeRepository.containsKey(jsonObject.toString())){
					dataTypeRepository.put(jsonObject.toString(), new ConcurrentHashMap<String, DataType>());
				}
			}
		}
		return dataTypeRepository.get(jsonObject.toString());
	}

}
