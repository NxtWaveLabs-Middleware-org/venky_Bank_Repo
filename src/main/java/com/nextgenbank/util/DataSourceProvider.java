package com.nextgenbank.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Centralizes the JNDI lookup so every DAO shares one code path.
 * This is intentionally the exact lookup that Session 9 (JNDI Naming) breaks
 * and fixes: java:comp/env/jdbc/nextgenbankDS maps via web.xml's resource-ref
 * to the physical WAS DataSource bound at jdbc/nextgenbankDS.
 */
public class DataSourceProvider {

    private static volatile DataSource dataSource;

    public static DataSource get() throws NamingException {
        if (dataSource == null) {
            synchronized (DataSourceProvider.class) {
                if (dataSource == null) {
                    Context ctx = new InitialContext();
                    dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/nextgenbankDS");
                }
            }
        }
        return dataSource;
    }
}
