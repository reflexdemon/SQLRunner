package org.reflexdemon.util;

import java.sql.*;
import java.util.*;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
public final class DBUtils {
   static Log log = LogFactory.getLog(DBUtils.class);

   public static final void close(Connection con, Statement state, ResultSet rs) {
      close(rs);
      close(state);
      close(con);
   }

   /** */
   public static final void close(Statement state) {
      if (state != null) {
         try {
            state.close();
         } catch (Exception ex) {
            log.error("Error closing statement: ", ex);
         }
      }
   }

   /** */
   public static final void close(ResultSet rs) {
      if (rs != null) {
         try {
            rs.close();
         } catch (Exception ex) {
            log.error("Error closing ResultSet: ", ex);
         }
      }
   }

   /** */
   public static final void close(Connection con) {
      if (con != null) {
         try {
            if (!con.isClosed())
               con.close();
         } catch (Exception ex) {
            log.error("Error closing connection: ", ex);
         }
      }
   }

   /** */
   public static int intFunction(Statement state, String sql)
         throws SQLException {
      int result = 0;

      ResultSet rs = null;
      try {
         log.trace("intFunction(sql): " + sql);
         rs = state.executeQuery(sql);
         if (rs.next()) {
            result = rs.getInt(1);
         }
      } finally {
         DBUtils.close(rs);
      }
      return result;
   }
   
   /** */
   public static Date dateFunction(Statement state, String sql)
         throws SQLException {
      Date result=null;

      ResultSet rs = null;
      try {
         log.trace("dateFunction(sql): " + sql);
         rs = state.executeQuery(sql);
         if (rs.next()) {
            result = rs.getTimestamp(1);
         }
      } finally {
         DBUtils.close(rs);
      }
      return result;
   }

   
   
   /** */
   public static List<String> stringListFunction(Statement state, String sql)
         throws SQLException {
      List<String> list = new ArrayList<String>();

      ResultSet rs = null;
      try {
         log.trace("stringListFunction(sql): " + sql);
         rs = state.executeQuery(sql);
         while (rs.next()) {
            list.add(rs.getString(1));
         }
      } finally {
         DBUtils.close(rs);
      }
      return list;
   }

   public static final Connection getConnection(String driver, String url, String user,
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
}
