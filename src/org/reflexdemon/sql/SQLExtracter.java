package org.reflexdemon.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jmatrix.eproperties.EProperties;
import net.jmatrix.eproperties.cli.ArgParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reflexdemon.util.DBUtils;
import org.reflexdemon.util.Log4JLogConfig;
import org.reflexdemon.util.PropertyLoader;
import org.reflexdemon.util.StreamUtil;
import org.reflexdemon.util.TimeUtil;

/**
 * A command line tool for execuitng sql statements.
 * 
 * 
 */
public class SQLExtracter {
    static Log log = LogFactory.getLog(SQLExtracter.class);

    static String driver = "oracle.jdbc.driver.OracleDriver";

    static String usage = "SQLExtracter [-x] [--dryrun] config \n"
            + "    where\n"
            + " -x: continue on error.  default (no arg) WILL fail on error.\n"
            + " --dryrun: don't execute anything.  just connect to the DB, and log.\n"
            + " config : property file";

    static boolean failonerror = false;
    static boolean dryrun = false;

    /** */
    public static void main(String[] args) throws Exception {
        boolean headerAdded = false;
        EProperties p = new EProperties();

        ArgParser ap = new ArgParser(args);

        Log4JLogConfig.log4jBootstrap();
        dryrun = ap.getBooleanArg("--dryrun", dryrun);
        failonerror = ap.getBooleanArg("-x", failonerror);
        
        String config = ap.getLastArg();
        p.load(config);
        
        String url = p.getString("jdbc.url");
        String user = p.getString("jdbc.user");
        String pass = p.getString("jdbc.pass");
        int limit = p.getInt("jdbc.limit", 100);
        driver = p.getString("jdbc.driver", driver);
        
        
        String table = p.getString("sql.table");// "V_RESOURCE"
        String condition = p.getString("sql.condition");// "RESOURCE_KEY IS NOT NULL";
        String filterColumn = p.getString("sql.filterColumn");;//"ACCOUNT_NUMBER";
        String mysql = "SELECT * from " + table + " WHERE 1=1 AND " + condition
                + " " + filterColumn;

        log.debug("SQL finally built :" + mysql);

        
        String input = p.getString("sql.input");
        String output = p.getString("sql.output");
        

        if (url == null || user == null || pass == null || input == null) {
            System.out.println(usage);
            System.exit(1);
        }

        String sql = StreamUtil.readToString(new File(input));

        log.debug("SQL File " + input + " has " + sql.split("\\\n").length
                + " lines.");

        // String nocomment = stripSQLComments(sql);
        String nocomment = sql;

        log.debug("SQL w/o Commeents has " + nocomment.split("\\\n").length
                + " lines");

        List<String> statements = splitSQL(nocomment, "\n");
        int scount = statements.size();

        log.debug("There are " + scount + " Accounts");

        Connection con = null;
        Statement state = null;
        int totalrows = 0;
        int success = 0;
        int fail = 0;
        int empty = 0;

        int attempted = 0;

        long overallStart = System.currentTimeMillis();
        try {
            con = getConnection(driver, url, user, pass);
            int max = statements.size();
            if (max < limit) {
                limit = 1;// Increase by 1 will do
            }
            File outFile = new File(output);
            for (int i = 0; i < max; i += limit) {
                
                StringBuilder builder = new StringBuilder();
                int limitX = limit;
                if ((i + limit) > max) {
                    limitX = (max - i);// Safety for AIOB
                }
                String statement = getINStatement(statements, i, limitX);

                statement = mysql + " IN " + statement.trim();

                log.debug("Executing  [" + i + "] \n" + statement); // "\n-------------------------------");

                long start = System.currentTimeMillis();
                int rows = 0;
                try {
                    
                    state = con.createStatement();

                    ResultSet rs = null;
                    if (!dryrun) {
                        attempted++;

                        rs = state.executeQuery(statement);
                        int columns = rs.getMetaData().getColumnCount();
                        if (!headerAdded) {
                            headerAdded = !headerAdded;
                            String header = getDelimitedHeaders(rs.getMetaData());
                            StreamUtil.write(header.getBytes(), outFile);
                        }
                        rows = 0;
                        while (rs.next()) {
                            rows++;
                            String line = getDelimitedData(rs, "|", columns);
                            // log.debug("line" + line);
                            builder.append(line).append("\n");
                        }


                        StreamUtil
                                .write(builder.toString().getBytes(), outFile, true);//Append

                    }

                    totalrows = totalrows + rows;

                    long et = System.currentTimeMillis() - start;

                    log.debug("Finished [" + i + "], rows=" + rows + ", et="
                            + et + "ms");
                    success++;

                } catch (SQLException ex) {
                    log.error("Error: " + ex);
                    fail++;

                    if (failonerror) {
                        throw new Error("Failed to execute statement " + i
                                + " of " + scount);
                    }
                } finally {
                    DBUtils.close(state);

                    System.out
                            .println("################################################");
                }
            }

        } finally {
            DBUtils.close(con);
            System.out.println("\n");
            System.out.println("==========================================");
            System.out.println("   Executed " + attempted + " of " + scount
                    + " statements.");
            System.out.println("      Success: " + success);
            System.out.println("      Fail   : " + fail);
            System.out.println("      Empty  : " + empty);

            System.out.println("   Total ET: "
                    + TimeUtil.formatElapsedTime(System.currentTimeMillis()
                            - overallStart));
            System.out.println("   Total rows: " + totalrows);
            System.out.println("==========================================");
        }

    }

    /**
     * Gets the delimited headers.
     *
     * @param metaData the meta data
     * @return the delimited headers
     * @throws SQLException the SQL exception
     */
    private static String getDelimitedHeaders(ResultSetMetaData metaData) throws SQLException {
        StringBuilder builder = new StringBuilder();
        int columns = metaData.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            builder.append(metaData.getColumnName(i));
            if (i < columns) {
                builder.append("|");
            }
        }
        return builder.toString();
    }

    /**
     * Gets the delimited line.
     * 
     * @param rs
     *            the rs
     * @param delimiter
     *            the delimiter
     * @param columns
     *            the columns
     * @return the delimited line
     * @throws SQLException
     *             the SQL exception
     */
    private static String getDelimitedData(ResultSet rs, String delimiter,
            int columns) throws SQLException {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= columns; i++) {
            builder.append(rs.getString(i));
            if (i < columns) {
                builder.append("|");
            }
        }
        return builder.toString();
    }

    /**
     * Gets the IN statement.
     * 
     * @param statements
     *            the statements
     * @param start
     *            the start
     * @param limit
     *            the limit
     * @return the IN statement
     */
    private static String getINStatement(List<String> statements, int start,
            int limit) {
        StringBuilder builder = new StringBuilder();
//        log.debug("Start:" + start + ", Limit:" + limit);
        int maxLoop = (start + limit);
        log.debug("List Count " + (maxLoop - start));
        builder.append("(");
        for (int i = start; i < maxLoop; i++) {
            builder.append("\'").append(statements.get(i).trim()).append("\'");
            if ((i + 1) != maxLoop) {
                builder.append(",");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    static Connection getConnection(String driver, String url, String user,
            String pass) throws SQLException {
        try {
            log.debug("Connecting to " + url + " as " + user);

            Class.forName(driver);
            Connection con = DriverManager.getConnection(url, user, pass);

            return con;
        } catch (Exception ex) {
            throw new SQLException("Error getting connection...", ex);
        }
    }

    /** */
    static String stripSQLComments(String sql) {
        log.debug("SQL before comment removal, length is " + sql.length());

        String multiLineRegex = "/\\*(.)*?\\*/";
        // This regex used to be: "/\\*(.|[\\r\\n])*?\\*/";
        // I changed it due to a stack overflow caused by a bug in the java
        // regular expressions. The bug reports here:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5050507
        // and here
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337993
        // They suggest not using the | construct. The . normally matches
        // any character except line terminators. But by adding the
        // Pattern.DOTALL
        // flag, the dot match includes line terminators, and we can remove the
        // |
        // Bemo, 25 Nov 2009.
        Pattern multiLinePattern = Pattern.compile(multiLineRegex,
                Pattern.DOTALL);
        Matcher matcher = multiLinePattern.matcher(sql);

        sql = matcher.replaceAll("");

        log.debug("SQL after multiline comment removal, length is "
                + sql.length());

        String singleLineRegex = "^(\\s)*\\-\\-.*?$";
        Pattern singleLinePattern = Pattern.compile(singleLineRegex,
                Pattern.MULTILINE);
        matcher = singleLinePattern.matcher(sql);

        sql = matcher.replaceAll("");
        log.debug("SQL after single line comment removal, length is "
                + sql.length());

        return sql;
    }

    /** */
    static List<String> splitSQL(String sql, String delimiter) {

        String split[] = sql.split(delimiter);

        Set<String> unique = new HashSet<String>(Arrays.asList(split));
        
        return new ArrayList<String>(unique);
    }
}
