package com.pti.fsc.wfe.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jbpm.services.task.identity.AbstractUserGroupInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfeUserBuCallback extends AbstractUserGroupInfo {
	
	private static final Logger logger = LoggerFactory.getLogger(WfeUserBuCallback.class);
    
    protected static final String DEFAULT_PROPERTIES_NAME = "classpath:/jbpm.usergroup.callback.properties";
    
    public static final String DS_JNDI_NAME = "db.ds.jndi.name";
    public static final String USER_BUS_QUERY = "db.user.bus.query";
    
    private Properties config;
    private DataSource ds;
    
    //no no-arg constructor to prevent cdi from auto deploy
    public WfeUserBuCallback() {
        String propertiesLocation = System.getProperty("jbpm.usergroup.callback.properties");        
        config = readProperties(propertiesLocation, DEFAULT_PROPERTIES_NAME);
        init();
    }
    
    public WfeUserBuCallback(Properties config) {
        this.config = config;        
        init();
    }

	public List<String> getBusForUser(String userId, String siteCode) {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if(siteCode == null || siteCode.length() == 0) {
			siteCode = userId.substring(0, userId.indexOf(":"));
		}
		userId = userId.substring(userId.indexOf(":")+1, userId.length());
		List<String> roles = new ArrayList<String>();
		Connection conn = null;
		CallableStatement cs = null;
		ResultSet rs = null;
		try {
			conn = ds.getConnection();
			if (logger.isTraceEnabled()) {
				logger.trace("========UserID : " + userId);
				logger.trace("========USER_ROLES_QUERY =" + this.config.getProperty(USER_BUS_QUERY));
			}
			cs = conn.prepareCall(this.config.getProperty(USER_BUS_QUERY));
			try {
				cs.registerOutParameter(1, -10);
				cs.setString(2, userId);
				cs.setString(3, siteCode);
				cs.execute();
			} catch (ArrayIndexOutOfBoundsException ignore) {
				
			}
			rs = (ResultSet)cs.getObject(1);
			while (rs.next()) {
				roles.add(rs.getString(1));
			}
		} catch (Exception e) {
			logger.error("Error when checking roles in db, parameter: " + userId, e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ex) {
				}
			}
		}
		
		return roles;
	}
	
	protected Connection getConnection() throws SQLException {
		return ds.getConnection();
	}
	
	private void init() {
		if (this.config == null || !this.config.containsKey(DS_JNDI_NAME) 
				|| !this.config.containsKey(USER_BUS_QUERY)) {
			throw new IllegalArgumentException("All properties must be given ("+ DS_JNDI_NAME + ","
					+ USER_BUS_QUERY +")");
		}
		String jndiName = this.config.getProperty(DS_JNDI_NAME, "java:/DefaultDS");
		if (logger.isTraceEnabled()) {
			logger.trace("============ DS_JNDI_NAME : " + this.config.getProperty(DS_JNDI_NAME));
			logger.trace("============ USER_BUS_QUERY : " + this.config.getProperty(USER_BUS_QUERY));
		}
		try {
			InitialContext ctx = new InitialContext();
			ds = (DataSource) ctx.lookup(jndiName);
		} catch (Exception e) {
			throw new IllegalStateException("Can get data source for DB usergroup callback, JNDI name: " + jndiName, e);
		}
	}
	
	public boolean needCheckBU(String userId, String siteCode, boolean wfeCheckBuFlag) {
		boolean needFilterByBu = false;
		if(userId != null && userId.contains(":")) {
			if(wfeCheckBuFlag){
				String workCompanyCode = userId.substring(0, userId.indexOf(":"));
				if(workCompanyCode.equals(siteCode)) {
					needFilterByBu = true;
				}
			}
		}
		return needFilterByBu;
	}

}
