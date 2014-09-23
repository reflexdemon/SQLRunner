package org.reflexdemon.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jmatrix.eproperties.cli.ArgParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reflexdemon.util.DBUtils;
import org.reflexdemon.util.Log4JLogConfig;
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

    static String usage = "SQLRunner2 [-d driver] [-x] [--dryrun] [-o outfile] -c url -u user -p pass -t tableName -f additionalFilter -i keyColumn <input file> \n"
            + "    where\n"
            + " -x: continue on error.  default (no arg) WILL fail on error.\n"
            + " --dryrun: don't execute anything.  just connect to the DB, and log.\n"
            + " -o: redirect resultset to file; defaults to '/tmp/output.txt'\n"
            + " -t: table or view name\n"
            + " -f: Additional filter like 'KEY is NOT NULL'\n"
            + " -i: Primary filter column used for IN operator like 'ID'\n"
            + " -l: Number of accounts to fetch in single shot; defaults to '100'";

    static boolean failonerror = false;
    static boolean dryrun = false;

    /** */
    public static void main(String[] args) throws Exception {
        Log4JLogConfig.log4jBootstrap();

        ArgParser ap = new ArgParser(args);

        String url = ap.getStringArg("-c");
        String user = ap.getStringArg("-u");
        String pass = ap.getStringArg("-p");
        String output = ap.getStringArg("-o", "/tmp/output.txt");
        int limit = ap.getIntArg("-l", 100);
        driver = ap.getStringArg("-d", driver);

        String table = ap.getStringArg("-t");// "V_RESOURCE"
        String condition = ap.getStringArg("-f");// "RESOURCE_KEY IS NOT NULL";
        String filterColumn = "ACCOUNT_NUMBER";
        String mysql = "SELECT * from " + table + " WHERE 1=1 AND " + condition
                + " AND " + filterColumn;

        log.debug("SQL finally built :" + mysql);

        dryrun = ap.getBooleanArg("--dryrun", dryrun);
        failonerror = ap.getBooleanArg("-x", failonerror);

        String input = ap.getLastArg();

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
                        rows = 0;
                        while (rs.next()) {
                            rows++;
                            String line = getDelimitedLine(rs, "|", columns);
                            // log.debug("line" + line);
                            builder.append(line).append("\n");
                        }

                        File outFile = new File(output);
                        StreamUtil
                                .write(builder.toString().getBytes(), outFile);

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
            DBUtils.close(con, state, null);

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
    private static String getDelimitedLine(ResultSet rs, String delimiter,
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

        return Arrays.asList(split);
    }
}
