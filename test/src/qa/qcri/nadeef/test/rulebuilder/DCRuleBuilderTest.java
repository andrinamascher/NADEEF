package qa.qcri.nadeef.test.rulebuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import qa.qcri.nadeef.core.datamodel.DataType;
import qa.qcri.nadeef.core.datamodel.Schema;
import qa.qcri.nadeef.core.util.Bootstrap;
import qa.qcri.nadeef.core.util.DataTypeRepository;
import qa.qcri.nadeef.ruleext.DCRuleBuilder;

import com.google.common.io.Files;


/**
 * Test on DC Rule
 * @author yye
 * 
 */

@RunWith(JUnit4.class)
public class DCRuleBuilderTest {
	
	private static File workingDirectory; 
	
    @Before
    public void setup() {
        Bootstrap.start();
        workingDirectory = Files.createTempDir();
    }

    @After
    public void tearDown() {
        Bootstrap.shutdown();
        workingDirectory.delete();
    }
    
    @Test
    public void testParser() throws Exception{
    	Schema.Builder schemaBuilder = new Schema.Builder();
    	List<String> values = new ArrayList<String>();
    	File output = null;
    	values.add("not(t1.C=t2.C&t1.A!=t2.A)");
    	values.add("not(t1.C>t1.B)");
    	values.add("not(t1.D=t2.D)");
    	values.add("not(t1.A='TEST')");
    	values.add("not(t1.B<=1)");
    	for (int i = 0; i < values.size(); i ++){
    		DCRuleBuilder dcRuleBuilder = new DCRuleBuilder();
    		String str = values.get(i);
    		Schema schema = schemaBuilder.table("table").column("A").column("B").column("C").column("D").build();
    		Map<String, DataType> map = DataTypeRepository.getTypeMapping("table");
    		map.put("A", DataType.STRING);
    		map.put("B", DataType.INTEGER);
    		map.put("C", DataType.DOUBLE);
    		map.put("D", DataType.STRING);
    		output = dcRuleBuilder.name("dc"+i).table("table").value(str).out(workingDirectory).compile().iterator().next();
    		System.out.println("Write file in " + output.getAbsolutePath());
    	}
    }
    
    @Test
    public void testInvalidParser() throws Exception {
    	Schema.Builder schemaBuilder = new Schema.Builder();
    	List<String> values = new ArrayList<String>();
    	File output = null;
    	values.add("not(C=C");
    	values.add("not(t1.C)");
    	values.add("not(t1.C!t2.C");
    	values.add("()");
    	for (int i = 0; i < values.size(); i ++){
    		DCRuleBuilder dcRuleBuilder = new DCRuleBuilder();
    		String str = values.get(i);
    		Schema schema = schemaBuilder.table("table").column("A").column("B").column("C").column("D").build();
    		Map<String, DataType> map = DataTypeRepository.getTypeMapping("table");
    		map.put("A", DataType.STRING);
    		map.put("B", DataType.INTEGER);
    		map.put("C", DataType.DOUBLE);
    		map.put("D", DataType.STRING);
    		try {
				output = dcRuleBuilder.name("dc"+i).table("table").value(str).out(workingDirectory).compile().iterator().next();
			} catch (Exception e) {
				if (e instanceof IllegalArgumentException){
					System.out.println(e.getMessage());
				}else{
					throw e;
				}
			}
    	}
    }
    
    @Test
    public void testViolationDetection(){
    	
    }
    
}
