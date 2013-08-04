package qa.qcri.nadeef.ruleext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import qa.qcri.nadeef.core.datamodel.NadeefConfiguration;
import qa.qcri.nadeef.core.datamodel.Operation;
import qa.qcri.nadeef.core.datamodel.Predicate;
import qa.qcri.nadeef.core.util.DataTypeRepository;
import qa.qcri.nadeef.core.util.RuleBuilder;
import qa.qcri.nadeef.tools.CommonTools;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Template engine for DC rule
 */
public class DCRuleBuilder extends RuleBuilder {

	private List<Predicate> predicateList;

	protected STGroupFile singleSTGroup;

	protected STGroupFile pairSTGroup;
	

	public DCRuleBuilder() {
		predicateList = new ArrayList<Predicate>();	
	}

	@Override
	public Collection<File> compile() throws Exception {
		File inputFile = generate();
		String fullPath = inputFile.getAbsolutePath();
		File classFile = new File(fullPath.replace(".java", ".class"));
		boolean alwaysCompile = NadeefConfiguration.getAlwaysCompile();
		if (alwaysCompile || !classFile.exists()) {
			CommonTools.compileFile(inputFile);
		}
		return Lists.newArrayList(classFile);
	}

	protected File generate() throws IOException {
		singleSTGroup = new STGroupFile(
				"qa/qcri/nadeef/ruleext/template/SingleDCRuleBuilder.stg", '$',
				'$');
		pairSTGroup = new STGroupFile(
				"qa/qcri/nadeef/ruleext/template/PairDCRuleBuilder.stg", '$',
				'$');
		ST st = null;
		if (isSingle()) {
			st = singleSTGroup.getInstanceOf("dcTemplate");
		} else {
			st = pairSTGroup.getInstanceOf("dcTemplate");
		}

		if (Strings.isNullOrEmpty(ruleName)) {
			ruleName = "DefaultDC" + CommonTools.toHashCode(value.get(0));
		} else {
			// remove all the empty spaces to make it a valid class name.
			ruleName = originalRuleName.replace(" ", "");
		}
		// escape all the double quote
		String template = toPredicateList().toString().replaceAll("\"", "\\\\\"");
		st.add("DCName", ruleName);
		st.add("template", template);
		File outputFile = getOutputFile();
		st.write(outputFile, null);
		return outputFile;
	}

	@SuppressWarnings("unchecked")
	private JSONArray toPredicateList() {
		JSONArray array = new JSONArray();
		for (Predicate predicate : predicateList) {
			JSONObject object = predicate.toJSON();
			array.add(object);
		}
		return array;
	}

	private boolean isSingle() {
		boolean isSingle = true;
		for (Predicate predicate : predicateList) {
			if (predicate.isSingle() == false) {
				isSingle = false;
				break;
			}
		}
		return isSingle;
	}

	@Override
	protected void parse() {
		// we assume rule has only one line
		if (value == null || value.size() > 1){
			throw new IllegalArgumentException("DC must be formulized in single line");
		}
		String line = value.get(0);
		if (!line.startsWith("not(")){
			throw new IllegalArgumentException("illegal header:"
					+ line);
		}
		if (!line.endsWith(")")){
			throw new IllegalArgumentException("illegal footer:"
					+ line);
		}
		String predicatesStr = line.substring(line.indexOf("not(") + 1,
				line.indexOf(")"));
		String[] predicates = predicatesStr.split("&");
		String patternStr = "([^>=<!]+)(>|>=|=|<=|<|!=)([^>=<!]+)";
		Pattern pattern = Pattern.compile(patternStr);
		for (int i = 0; i < predicates.length; i++) {
			String tmp = predicates[i];
			Matcher m = pattern.matcher(tmp);
			if (m.find()) {
				Predicate predicate = new Predicate();
				String leftHandSide = m.group(1);
				String operationStr = m.group(2);
				String rightHandSide = m.group(3);
				String leftColumnName = null;
				String rightColumnName = null;
				Operation operation = Predicate
						.getMappedOperation(operationStr);
				Pattern leftPattern = Pattern.compile("t1\\.(.*)");
				Matcher mLeft = leftPattern.matcher(leftHandSide);
				if (mLeft.find()) {
					leftColumnName = mLeft.group(1);
				} else {
					throw new IllegalArgumentException("illegal syntax"
							+ leftHandSide);
				}
				Pattern singleRightPattern = Pattern.compile("t1\\.(.*)");
				Matcher mSingleRightMatcher = singleRightPattern
						.matcher(rightHandSide);
				Pattern doubleRightPattern = Pattern.compile("t2\\.(.*)");
				Matcher mDoubleRightMatcher = doubleRightPattern
						.matcher(rightHandSide);
				boolean isRightConstant = false;
				boolean isSingle = false;
				if (mSingleRightMatcher.find()) {
					rightColumnName = mSingleRightMatcher.group(1);
					isSingle = true;
					isRightConstant = false;
				} else if (mDoubleRightMatcher.find()) {
					rightColumnName = mDoubleRightMatcher.group(1);
					isSingle = false;
					isRightConstant = false;
				} else {
					rightColumnName = rightHandSide;
					isRightConstant = true;
					isSingle = true;
				}
				
				
				predicate.setOp(operation);
				predicate.setLeft(leftColumnName);
				predicate.setSingle(isSingle);
				predicate.setRightConstant(isRightConstant);
				predicate.setRight(rightColumnName);
				predicateList.add(predicate);
			} else {
				throw new IllegalArgumentException("illegal syntax " + tmp);
			}
		}
	}
}
