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

package qa.qcri.nadeef.core.pipeline;

import com.google.common.base.Preconditions;
import qa.qcri.nadeef.core.datamodel.NadeefConfiguration;
import qa.qcri.nadeef.core.datamodel.Rule;
import qa.qcri.nadeef.core.datamodel.Violation;
import qa.qcri.nadeef.core.util.Violations;
import qa.qcri.nadeef.core.util.sql.DBConnectionPool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;

/**
 * Import violations from violation table.
 */
public class ViolationImport extends Operator<Rule, Collection<Violation>> {
    private DBConnectionPool connectionPool;

    ViolationImport(DBConnectionPool connectionPool) {
        this.connectionPool = Preconditions.checkNotNull(connectionPool);
    }

    /**
     * Execute the operator.
     *
     * @param rule input rule.
     * @return output violations from database.
     */
    @Override
    public Collection<Violation> execute(Rule rule) throws Exception {
        Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        Collection<Violation> result = null;
        try {
            conn = connectionPool.getNadeefConnection();
            conn.setAutoCommit(true);
            stat = conn.createStatement();
            resultSet = stat.executeQuery(
                "SELECT * FROM " +
                    NadeefConfiguration.getViolationTableName() +
                    " WHERE RID = '" +
                    rule.getRuleName() +
                    "' ORDER BY vid"
            );

            result = Violations.fromQuery(resultSet);
            setPercentage(1f);
        } finally {
            if (stat != null) {
                stat.close();
            }

            if (resultSet != null) {
                resultSet.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return result;
    }
}
