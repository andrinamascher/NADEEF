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
import com.google.common.collect.Maps;
import qa.qcri.nadeef.core.datamodel.Cell;
import qa.qcri.nadeef.core.datamodel.Column;
import qa.qcri.nadeef.core.datamodel.Fix;
import qa.qcri.nadeef.core.datamodel.NadeefConfiguration;
import qa.qcri.nadeef.core.util.sql.DBConnectionPool;
import qa.qcri.nadeef.tools.CommonTools;
import qa.qcri.nadeef.tools.DBConfig;
import qa.qcri.nadeef.tools.Tracer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * Updater fixes the source data and exports it in the database.
 * It returns <code>True</code> when there is no Cell changed in
 * the pipeline. In this case the pipeline will stop.
 */
public class Updater extends Operator<Collection<Fix>, Integer> {
    private static Tracer tracer = Tracer.getTracer(Updater.class);
    private ConcurrentMap<Cell, String> updateHistory;
    private ConcurrentMap<Cell, Boolean> unknownTag;
    private DBConfig sourceConfig;
    private DBConfig nadeefConfig;

    /**
     * Constructor.
     */
    public Updater(DBConfig sourceConfig, DBConfig nadeefConfig) {
        this.sourceConfig= Preconditions.checkNotNull(sourceConfig);
        this.nadeefConfig = Preconditions.checkNotNull(nadeefConfig);
        updateHistory = Maps.newConcurrentMap();
        unknownTag = Maps.newConcurrentMap();
    }

    /**
     * Apply the fixes from EQ and modify the original database.
     *
     * @param fixes Fix collection.
     * @return output object.
     */
    @Override
    public Integer execute(Collection<Fix> fixes) throws Exception {
        int count = 0;
        Connection sourceConn = null;
        Connection nadeefConn = null;
        Statement sourceStat = null;
        PreparedStatement auditStat = null;
        String auditTableName = NadeefConfiguration.getAuditTableName();
        String rightValue;
        String oldValue;

        try {
            nadeefConn = DBConnectionPool.createConnection(nadeefConfig);
            sourceConn = DBConnectionPool.createConnection(sourceConfig);
            sourceStat = sourceConn.createStatement();
            auditStat =
                nadeefConn.prepareStatement(
                    "INSERT INTO " + auditTableName +
                    " VALUES (default, ?, ?, ?, ?, ?, ?, current_timestamp)");
            for (Fix fix : fixes) {
                Cell cell = fix.getLeft();
                oldValue = cell.getValue().toString();

                // this cell has already been changed to unknown
                if (unknownTag.containsKey(cell)) {
                    continue;
                }

                // check whether this cell has been changed before
                if (updateHistory.containsKey(cell)) {
                    String value = updateHistory.get(cell);
                    if (value.equals(fix.getRightValue())) {
                        continue;
                    }
                    // when a cell is set twice with different value,
                    // we set it to null for ambiguous value.
                    unknownTag.put(cell, true);
                    rightValue = "?";
                } else {
                    rightValue = fix.getRightValue();
                    updateHistory.put(cell, rightValue);
                }

                // check for numerical type.
                if (!CommonTools.isNumericalString(rightValue)) {
                    rightValue = '\'' + rightValue + '\'';
                }

                if (!CommonTools.isNumericalString(oldValue)) {
                    oldValue = '\'' + oldValue + '\'';
                }

                Column column = cell.getColumn();
                String tableName = column.getTableName();
                String updateSql =
                    "UPDATE " + tableName +
                    " SET " + column.getColumnName() + " = " + rightValue +
                    " WHERE tid = " + cell.getTupleId();
                tracer.verbose(updateSql);
                sourceStat.addBatch(updateSql);
                auditStat.setInt(1, fix.getVid());
                auditStat.setInt(2, cell.getTupleId());
                auditStat.setString(3, column.getTableName());
                auditStat.setString(4, column.getColumnName());
                auditStat.setString(5, oldValue);
                auditStat.setString(6, rightValue);
                auditStat.addBatch();
                if (count % 4096 == 0) {
                    auditStat.executeBatch();
                    nadeefConn.commit();
                    sourceStat.executeBatch();
                    sourceConn.commit();
                }
                count ++;
                setPercentage(count / fixes.size());
            }
            sourceStat.executeBatch();
            auditStat.executeBatch();
            sourceConn.commit();
            nadeefConn.commit();
            Tracer.appendMetric(Tracer.Metric.UpdatedCellNumber, count);
        } finally {
            if (auditStat != null) {
                auditStat.close();
            }

            if (sourceStat != null) {
                sourceStat.close();
            }

            if (nadeefConn != null) {
                nadeefConn.close();
            }

            if (sourceConn != null) {
                sourceConn.close();
            }
        }
        return count;
    }
}
