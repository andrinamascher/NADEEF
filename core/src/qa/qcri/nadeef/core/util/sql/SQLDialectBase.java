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

package qa.qcri.nadeef.core.util.sql;

import com.google.common.base.Preconditions;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;
import qa.qcri.nadeef.tools.DBConfig;
import qa.qcri.nadeef.tools.sql.SQLDialect;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Interface for cross vendor Database methods.
 */
public abstract class SQLDialectBase {
    /**
     * Creates SQLDialect instance.
     * @param dialect dialect.
     * @return SQLDialectBase instance.
     */
    public static SQLDialectBase createDialectBaseInstance(SQLDialect dialect) {
        SQLDialectBase dialectInstance;
        switch (dialect) {
            default:
            case DERBYMEMORY:
            case DERBY:
                dialectInstance = new DerbySQLDialect();
                break;
            case POSTGRES:
                dialectInstance = new PostgresSQLDialect();
                break;
            case MYSQL:
                dialectInstance = new MySQLDialect();
                break;
        }
        return dialectInstance;
    }

    /**
     * Returns True when bulk loading is supported.
     * @return True when bulk loading is supported.
     */
    public boolean supportBulkLoad() {
        return false;
    }

    /**
     * Bulk load CSV file.
     * @param dbConfig DBConfig.
     * @param tableName table name.
     * @param file CSV file.
     * @param hasHeader has header.
     * @return line of rows loaded.
     */
    public int bulkLoad(
        DBConfig dbConfig,
        String tableName,
        Path file,
        boolean hasHeader
    ) {
        throw new UnsupportedOperationException("Method is not implemented.");
    }

    /**
     * Gets the template file.
     * @return template group file.
     */
    protected abstract STGroupFile getTemplate();

    /**
     * Install violation tables.
     * @param violationTableName violation table name.
     * @return SQL statement.
     */
    public String createViolationTable(String violationTableName) {
        STGroupFile template = Preconditions.checkNotNull(getTemplate());
        ST st = template.getInstanceOf("InstallViolationTable");
        st.add("violationTableName", violationTableName.toUpperCase());
        return st.render();
    }

    /**
     * Install repair tables.
     * @param repairTableName repair table name.
     * @return SQL statement.
     */
    public String createRepairTable(String repairTableName) {
        STGroupFile template = Preconditions.checkNotNull(getTemplate());
        ST st = template.getInstanceOf("InstallRepairTable");
        st.add("repairTableName", repairTableName.toUpperCase());
        return st.render();
    }

    /**
     * Install auditing tables.
     * @param auditTableName audit table name.
     * @return SQL statement.
     */
    public String createAuditTable(String auditTableName) {
        STGroupFile template = Preconditions.checkNotNull(getTemplate());
        ST st = template.getInstanceOf("InstallAuditTable");
        st.add("auditTableName", auditTableName.toUpperCase());
        return st.render();
    }

    /**
     * Next Vid.
     * @param tableName violation table name.
     * @return SQL statement.
     */
    public String nextVid(String tableName) {
        STGroupFile template = Preconditions.checkNotNull(getTemplate());
        ST st = template.getInstanceOf("NextVid");
        st.add("tableName", tableName.toUpperCase());
        return st.render();
    }

    /**
     * Creates a table in the database from a CSV file header.
     * @param tableName table name.
     * @param content table description.
     * @return SQL statement.
     */
    public String createTableFromCSV(String tableName, String content) {
        STGroupFile template = Preconditions.checkNotNull(getTemplate());
        ST st = template.getInstanceOf("CreateTableFromCSV");
        st.add("tableName", tableName.toUpperCase());
        st.add("content", content);
        return st.render();
    }

    /**
     * Copy table.
     * @param conn connection.
     * @param sourceName source name.
     * @param targetName target name.
     */
    public abstract void copyTable(
        Connection conn,
        String sourceName,
        String targetName
    ) throws SQLException;

    /**
     * Drop table.
     * @param tableName drop table name.
     * @return SQL statement.
     */
    public String dropTable(String tableName) {
        return "DROP TABLE " + tableName;
    }

    /**
     * Drop index.
     * @param indexName index name.
     * @param tableName drop table name.
     * @return SQL statement.
     */
    public String dropIndex(String indexName, String tableName) {
        return "DROP INDEX " + indexName;
    }

    /**
     * Select star..
     * @param tableName table name.
     * @return SQL statement.
     */
    public String selectAll(String tableName) {
        return "SELECT * FROM " + tableName;
    }

    public String deleteAll(String tableName) {
        return "DELETE FROM " + tableName;
    }

    /**
     * Count the number of rows in the table.
     * @param tableName table name.
     * @return SQL statement.
     */
    public abstract String countTable(String tableName);

    /**
     * Limits the select.
     * @param row row number.
     * @return SQL statement.
     */
    public abstract String limitRow(int row);

    /**
     * Inserts values into a table from CSV row used for batch loading.
     * @param metaData column meta data.
     * @param tableName target table name.
     * @param row row value.
     * @return SQL statement.
     */
    public abstract String importFromCSV(
        ResultSetMetaData metaData,
        String tableName,
        String row
    );
}
