/*
 * QCRI, NADEEF LICENSE
 * NADEEF is an extensible, generalized and easy-to-deploy data cleaning platform built at QCRI.
 * NADEEF means "Clean" in Arabic
 *
 * Copyright (c) 2011-2013, Qatar Foundation for Education, Science and Community Development (on
 * behalf of Qatar Computing Research Institute) having its principle place of business in Doha,
 * Qatar with the registered address P.O box 5825 Doha, Qatar (hereinafter referred to as "QCRI")
 *
 * NADEEF has patent pending nevertheless the following is granted.
 * NADEEF is released under the terms of the MIT License, (http://opensource.org/licenses/MIT).
 */

package qa.qcri.nadeef.test.udf;

import com.google.common.collect.Lists;
import qa.qcri.nadeef.core.datamodel.*;

import java.util.Collection;
import java.util.List;

/**
 * Pair table test.
 */
public class MyRule5short extends PairTupleRule {

    /**
     * Detect rule with pair tuple.
     *
     * @param pair input tuple pair.
     * @return Violation set.
     */
    @Override
    public Collection<Violation> detect(TuplePair pair) {
        List<Violation> result = Lists.newArrayList();

        if (
            !pair.getLeft().get("FN varchar(255)").equals(pair.getLeft().get("FN varchar(255)"))
        ) {
            Violation violation = new Violation(ruleName);
            violation.addCell(pair.getLeft().getCell("FN varchar(255)"));
            violation.addCell(pair.getRight().getCell("FN varchar(255)"));
            result.add(violation);
        }
        return result;
    }

    /**
     * Repair of this rule.
     *
     * @param violation violation input.
     * @return a candidate fix.
     */
    @Override
    public Collection<Fix> repair(Violation violation) {
        Fix.Builder builder = new Fix.Builder();
        Cell tran = violation.getCell("tran1", "FN");
        Cell bank = violation.getCell("bank1", "FN");
        Fix fix = builder.left(tran).right(bank.getValue().toString()).build();
        return Lists.newArrayList(fix);
    }
}
