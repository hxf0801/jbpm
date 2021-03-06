package org.jbpm.persistence.processinstance;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ProcInstanceProp")
public class ProcessInstanceExtra implements Serializable {
	/**
	 * default serial version UID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * internal defined column prefix with which variables starting should be
	 * saved in this extra table. Currently the process variable table also
	 * keeps one copy
	 */
	public static final String COLUMN_PREFIX = "EXT_";

	@Id
	@Column(name = "process_instance_id")
	private Long processInstanceId;

	@Column(name = "site_code")
	private String siteCode;

	@Column(name = "service_code")
	private String serviceCode;

	@Column(name = "company_code")
	private String companyCode;

	@Column(name = "process_group")
	private String processGroup;

	@Column(name = "item_key")
	private String itemKey;

	@Column(name = "item_type")
	private String itemType;

	@Column(name = "opt_type")
	private String optType;

	@Column(name = "text1")
	private String text1;

	@Column(name = "text2")
	private String text2;

	@Column(name = "text3")
	private String text3;

	@Column(name = "text4")
	private String text4;

	@Column(name = "text5")
	private String text5;

	@Column(name = "char1", length = 1)
	private String char1;

	@Column(name = "char2", length = 1)
	private String char2;

	@Column(name = "money1", precision = 20, scale = 3)
	private BigDecimal money1;

	@Column(name = "money2", precision = 20, scale = 3)
	private BigDecimal money2;

	@Column(name = "money3", precision = 20, scale = 3)
	private BigDecimal money3;

	@Column(name = "integer1")
	private Long integer1;

	@Column(name = "integer2")
	private Long integer2;

	@Column(name = "decimal1", precision = 19, scale = 9)
	private BigDecimal decimal1;

	@Column(name = "decimal2", precision = 19, scale = 9)
	private BigDecimal decimal2;

	@Temporal(TemporalType.DATE)
	@Column(name = "date1")
	private Date date1;

	@Temporal(TemporalType.DATE)
	@Column(name = "date2")
	private Date date2;

	@Temporal(TemporalType.DATE)
	@Column(name = "date3")
	private Date date3;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "timestamp1")
	private Date timestamp1;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "timestamp2")
	private Date timestamp2;
	
	@Column(name = "wfe_client_id")
	private String wfeClientIdentifier;
	
	@Column(name = "bu_name")
	private String buName;

	public ProcessInstanceExtra() {

	}

	/**
	 * Construct a new extra table object based on a given map of value
	 * 
	 * @param id
	 *        - Long, process instance id
	 * @param parameters
	 *        - Map
	 */
	public ProcessInstanceExtra(Long id, Map<String, Object> parameters) {
		this.processInstanceId = id;
		if (null != parameters) {
			this.siteCode = (String)parameters.get(COLUMN_PREFIX + "site_code");
			this.serviceCode = (String)parameters.get(COLUMN_PREFIX + "service_code");
			this.companyCode = (String)parameters.get(COLUMN_PREFIX + "company_code");
			this.processGroup = (String)parameters.get(COLUMN_PREFIX + "process_group");
			this.itemKey = (String)parameters.get(COLUMN_PREFIX + "item_key");
			this.itemType = (String)parameters.get(COLUMN_PREFIX + "item_type");
			this.optType = (String)parameters.get(COLUMN_PREFIX + "opt_type");

			this.text1 = (String)parameters.get(COLUMN_PREFIX + "text1");
			this.text2 = (String)parameters.get(COLUMN_PREFIX + "text2");
			this.text3 = (String)parameters.get(COLUMN_PREFIX + "text3");
			this.text4 = (String)parameters.get(COLUMN_PREFIX + "text4");
			this.text5 = (String)parameters.get(COLUMN_PREFIX + "text5");
			this.char1 = (String)parameters.get(COLUMN_PREFIX + "char1");
			this.char2 = (String)parameters.get(COLUMN_PREFIX + "char2");
			
			if (null != parameters.get(COLUMN_PREFIX + "money1")) {
				this.money1 = this.convertTo(parameters.get(COLUMN_PREFIX + "money1"));
			}
			if (null != parameters.get(COLUMN_PREFIX + "money2")) {
				this.money2 = this.convertTo(parameters.get(COLUMN_PREFIX + "money2"));
			}
			if (null != parameters.get(COLUMN_PREFIX + "money3")) {
				this.money3 = this.convertTo(parameters.get(COLUMN_PREFIX + "money3"));
			}
			
			this.integer1 = (Long)parameters.get(COLUMN_PREFIX + "integer1");
			this.integer2 = (Long)parameters.get(COLUMN_PREFIX + "integer2");
			if (null != parameters.get(COLUMN_PREFIX + "decimal1")) {
				this.decimal1 = this.convertTo(parameters.get(COLUMN_PREFIX + "decimal1"));
			}
			if (null != parameters.get(COLUMN_PREFIX + "decimal2")) {
				this.decimal2 = this.convertTo(parameters.get(COLUMN_PREFIX + "decimal2"));
			}
			this.date1 = (Date)parameters.get(COLUMN_PREFIX + "date1");
			this.date2 = (Date)parameters.get(COLUMN_PREFIX + "date2");
			this.date3 = (Date)parameters.get(COLUMN_PREFIX + "date3");
			this.timestamp1 = (Date)parameters.get(COLUMN_PREFIX + "timestamp1");
			this.timestamp2 = (Date)parameters.get(COLUMN_PREFIX + "timestamp2");
			this.wfeClientIdentifier = (String)parameters.get(COLUMN_PREFIX + "wfe_client_id");
			this.buName = (String)parameters.get(COLUMN_PREFIX + "bu_name");
		}
	}

	/**
	 * Update own state based on given values. It will be called to persist the
	 * update to the extra table when completing a task. So don't forget to add
	 * it here when adding a new field
	 * 
	 * @param updatedParameters
	 *        - Map
	 */
	public void updateState(Map<String, Object> updatedParameters) {
		if (null != updatedParameters) {
			if (null != updatedParameters.get(COLUMN_PREFIX + "site_code")) {
				this.siteCode = (String)updatedParameters.get(COLUMN_PREFIX + "site_code");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "service_code")) {
				this.serviceCode = (String)updatedParameters.get(COLUMN_PREFIX + "service_code");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "company_code")) {
				this.companyCode = (String)updatedParameters.get(COLUMN_PREFIX + "company_code");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "process_group")) {
				this.processGroup = (String)updatedParameters.get(COLUMN_PREFIX + "process_group");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "item_key")) {
				this.itemKey = (String)updatedParameters.get(COLUMN_PREFIX + "item_key");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "item_type")) {
				this.itemType = (String)updatedParameters.get(COLUMN_PREFIX + "item_type");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "opt_type")) {
				this.optType = (String)updatedParameters.get(COLUMN_PREFIX + "opt_type");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "text1")) {
				this.text1 = (String)updatedParameters.get(COLUMN_PREFIX + "text1");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "text2")) {
				this.text2 = (String)updatedParameters.get(COLUMN_PREFIX + "text2");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "text3")) {
				this.text3 = (String)updatedParameters.get(COLUMN_PREFIX + "text3");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "text4")) {
				this.text4 = (String)updatedParameters.get(COLUMN_PREFIX + "text4");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "text5")) {
				this.text5 = (String)updatedParameters.get(COLUMN_PREFIX + "text5");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "char1")) {
				this.char1 = (String)updatedParameters.get(COLUMN_PREFIX + "char1");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "char2")) {
				this.char2 = (String)updatedParameters.get(COLUMN_PREFIX + "char2");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "money1")) {
				this.money1 = this.convertTo(updatedParameters.get(COLUMN_PREFIX + "money1"));
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "money2")) {
				this.money2 = this.convertTo(updatedParameters.get(COLUMN_PREFIX + "money2"));
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "money3")) {
				this.money3 = this.convertTo(updatedParameters.get(COLUMN_PREFIX + "money3"));
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "integer1")) {
				this.integer1 = (Long)updatedParameters.get(COLUMN_PREFIX + "integer1");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "integer2")) {
				this.integer2 = (Long)updatedParameters.get(COLUMN_PREFIX + "integer2");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "decimal1")) {
				this.decimal1 = this.convertTo(updatedParameters.get(COLUMN_PREFIX + "decimal1"));
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "decimal2")) {
				this.decimal2 = this.convertTo(updatedParameters.get(COLUMN_PREFIX + "decimal2"));
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "date1")) {
				this.date1 = (Date)updatedParameters.get(COLUMN_PREFIX + "date1");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "date2")) {
				this.date2 = (Date)updatedParameters.get(COLUMN_PREFIX + "date2");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "date3")) {
				this.date3 = (Date)updatedParameters.get(COLUMN_PREFIX + "date3");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "timestamp1")) {
				this.timestamp1 = (Date)updatedParameters.get(COLUMN_PREFIX + "timestamp1");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "timestamp2")) {
				this.timestamp2 = (Date)updatedParameters.get(COLUMN_PREFIX + "timestamp2");
			}
			if (null != updatedParameters.get(COLUMN_PREFIX + "wfe_client_id")) {
				this.wfeClientIdentifier = (String)updatedParameters.get(COLUMN_PREFIX + "wfe_client_id");
			}
		}
	}

	private BigDecimal convertTo(Object obj) {
		if (obj instanceof BigDecimal)
			return (BigDecimal)obj;
		return new BigDecimal(obj.toString());
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	/**
	 * Check if this object is valid when persisting it. Only insert valid
	 * object.
	 * 
	 * @return boolean
	 */
	public boolean isValid() {
		return (null != siteCode || null != serviceCode || null != companyCode || null != processGroup);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ProcessInstanceExtra={");
		sb.append("processInstanceId:" + processInstanceId + ", ");
		sb.append("siteCode:" + (null != siteCode ? siteCode : "") + ", ");
		sb.append("serviceCode:" + (null != serviceCode ? serviceCode : "") + ", ");
		sb.append("companyCode:" + (null != companyCode ? companyCode : "") + ", ");
		sb.append("processGroup:" + (null != processGroup ? processGroup : "") + ", ");
		sb.append("itemKey:" + (null != itemKey ? itemKey : "") + ", ");
		sb.append("itemType:" + (null != itemType ? itemType : "") + ", ");
		sb.append("optType:" + (null != optType ? optType : "") + ", ");
		sb.append("text1:" + (null != text1 ? text1 : "") + ", ");
		sb.append("text2:" + (null != text2 ? text2 : "") + ", ");
		sb.append("text3:" + (null != text3 ? text3 : "") + ", ");
		sb.append("text4:" + (null != text4 ? text4 : "") + ", ");
		sb.append("text5:" + (null != text5 ? text5 : "") + ", ");
		sb.append("char1:" + (null != char1 ? char1 : "") + ", ");
		sb.append("char2:" + (null != char2 ? char2 : "") + ", ");
		sb.append("money1:" + (null != money1 ? money1 : 0) + ", ");
		sb.append("money2:" + (null != money2 ? money2 : 0) + ", ");
		sb.append("money3:" + (null != money3 ? money3 : 0) + ", ");
		sb.append("integer1:" + (null != integer1 ? integer1 : 0) + ", ");
		sb.append("integer2:" + (null != integer2 ? integer2 : 0) + ", ");
		sb.append("decimal1:" + (null != decimal1 ? decimal1 : 0) + ", ");
		sb.append("decimal2:" + (null != decimal2 ? decimal2 : 0) + ", ");
		sb.append("date1:" + (null != date1 ? date1 : "") + ", ");
		sb.append("date2:" + (null != date2 ? date2 : "") + ", ");
		sb.append("date3:" + (null != date3 ? date3 : "") + ", ");
		sb.append("timestamp1:" + (null != timestamp1 ? timestamp1 : "") + ", ");
		sb.append("timestamp2:" + (null != timestamp2 ? timestamp2 : "") + ", ");
		sb.append("wfeClientIdentifier:" + (null != wfeClientIdentifier ? wfeClientIdentifier : ""));
		sb.append("buName:" + (null != buName ? buName : ""));
		sb.append("}");
		return sb.toString();
	}

	public String getSiteCode() {
		return siteCode;
	}

	public void setSiteCode(String siteCode) {
		this.siteCode = siteCode;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public String getCompanyCode() {
		return companyCode;
	}

	public void setCompanyCode(String companyCode) {
		this.companyCode = companyCode;
	}

	public String getProcessGroup() {
		return processGroup;
	}

	public void setProcessGroup(String processGroup) {
		this.processGroup = processGroup;
	}

	public String getItemKey() {
		return itemKey;
	}

	public void setItemKey(String itemKey) {
		this.itemKey = itemKey;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public String getOptType() {
		return optType;
	}

	public void setOptType(String optType) {
		this.optType = optType;
	}

	public String getText1() {
		return text1;
	}

	public void setText1(String text1) {
		this.text1 = text1;
	}

	public String getText2() {
		return text2;
	}

	public void setText2(String text2) {
		this.text2 = text2;
	}

	public String getText3() {
		return text3;
	}

	public void setText3(String text3) {
		this.text3 = text3;
	}

	public String getText4() {
		return text4;
	}

	public void setText4(String text4) {
		this.text4 = text4;
	}

	public String getText5() {
		return text5;
	}

	public void setText5(String text5) {
		this.text5 = text5;
	}

	public String getChar1() {
		return char1;
	}

	public void setChar1(String char1) {
		this.char1 = char1;
	}

	public String getChar2() {
		return char2;
	}

	public void setChar2(String char2) {
		this.char2 = char2;
	}

	/**
	 * @return the money1
	 */
	public BigDecimal getMoney1() {
		return money1;
	}

	/**
	 * @param money1
	 *        the money1 to set
	 */
	public void setMoney1(BigDecimal money1) {
		this.money1 = money1;
	}

	/**
	 * @return the money2
	 */
	public BigDecimal getMoney2() {
		return money2;
	}

	/**
	 * @param money2
	 *        the money2 to set
	 */
	public void setMoney2(BigDecimal money2) {
		this.money2 = money2;
	}

	/**
	 * @return the money3
	 */
	public BigDecimal getMoney3() {
		return money3;
	}

	/**
	 * @param money3
	 *        the money3 to set
	 */
	public void setMoney3(BigDecimal money3) {
		this.money3 = money3;
	}

	public Long getInteger1() {
		return integer1;
	}

	public void setInteger1(Long integer1) {
		this.integer1 = integer1;
	}

	public Long getInteger2() {
		return integer2;
	}

	public void setInteger2(Long integer2) {
		this.integer2 = integer2;
	}

	/**
	 * @return the decimal1
	 */
	public BigDecimal getDecimal1() {
		return decimal1;
	}

	/**
	 * @param decimal1
	 *        the decimal1 to set
	 */
	public void setDecimal1(BigDecimal decimal1) {
		this.decimal1 = decimal1;
	}

	/**
	 * @return the decimal2
	 */
	public BigDecimal getDecimal2() {
		return decimal2;
	}

	/**
	 * @param decimal2
	 *        the decimal2 to set
	 */
	public void setDecimal2(BigDecimal decimal2) {
		this.decimal2 = decimal2;
	}

	public Date getDate1() {
		return date1;
	}

	public void setDate1(Date date1) {
		this.date1 = date1;
	}

	public Date getDate2() {
		return date2;
	}

	public void setDate2(Date date2) {
		this.date2 = date2;
	}

	public Date getDate3() {
		return date3;
	}

	public void setDate3(Date date3) {
		this.date3 = date3;
	}

	public Date getTimestamp1() {
		return timestamp1;
	}

	public void setTimestamp1(Date timestamp1) {
		this.timestamp1 = timestamp1;
	}

	public Date getTimestamp2() {
		return timestamp2;
	}

	public void setTimestamp2(Date timestamp2) {
		this.timestamp2 = timestamp2;
	}

	public String getWfeClientIdentifier() {
		return wfeClientIdentifier;
	}

	public void setWfeClientIdentifier(String wfeClientIdentifier) {
		this.wfeClientIdentifier = wfeClientIdentifier;
	}

	public String getBuName() {
		return buName;
	}

	public void setBuName(String buName) {
		this.buName = buName;
	}
	
}