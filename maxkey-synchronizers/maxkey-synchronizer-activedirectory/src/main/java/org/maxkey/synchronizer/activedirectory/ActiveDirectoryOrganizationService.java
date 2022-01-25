/*
 * Copyright [2021] [MaxKey of copyright http://www.maxkey.top]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 

package org.maxkey.synchronizer.activedirectory;

import java.util.ArrayList;
import java.util.HashMap;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang3.StringUtils;
import org.maxkey.constants.ConstsStatus;
import org.maxkey.constants.ldap.OrganizationalUnit;
import org.maxkey.entity.HistorySynchronizer;
import org.maxkey.entity.Organizations;
import org.maxkey.persistence.ldap.ActiveDirectoryUtils;
import org.maxkey.persistence.ldap.LdapUtils;
import org.maxkey.synchronizer.AbstractSynchronizerService;
import org.maxkey.synchronizer.ISynchronizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ActiveDirectoryOrganizationService  extends AbstractSynchronizerService  implements ISynchronizerService{
	final static Logger _logger = LoggerFactory.getLogger(ActiveDirectoryOrganizationService.class);

	ActiveDirectoryUtils ldapUtils;
	
	public void sync() {
	    loadOrgsById("1");
		_logger.info("Sync ActiveDirectory Organizations ...");
		try {
			SearchControls constraints = new SearchControls();
			constraints.setSearchScope(ldapUtils.getSearchScope());
			String filter = "(&(objectClass=OrganizationalUnit))";
			if(StringUtils.isNotBlank(this.getSynchronizer().getFilters())) {
				//filter = this.getSynchronizer().getFilters();
			}
			
			NamingEnumeration<SearchResult> results = 
						ldapUtils.getConnection().search(ldapUtils.getBaseDN(), filter, constraints);
			
			ArrayList<Organizations> orgsList = new ArrayList<Organizations>();
			int  maxLevel 		= 0;
			long recordCount 	= 0;
			while (null != results && results.hasMoreElements()) {
				Object obj = results.nextElement();
				if (obj instanceof SearchResult) {
					SearchResult sr = (SearchResult) obj;
					if("OU=Domain Controllers,DC=maxkey,DC=top".endsWith(sr.getNameInNamespace())) {
					    _logger.info("Skip  'OU=Domain Controllers' .");
					    continue;
					}
					_logger.debug("Sync OrganizationalUnit {} , name {} , NameInNamespace {}" , 
								    (++recordCount),sr.getName(),sr.getNameInNamespace());
					
					HashMap<String,Attribute> attributeMap = new HashMap<String,Attribute>();
					NamingEnumeration<? extends Attribute>  attrs = sr.getAttributes().getAll();
					while (null != attrs && attrs.hasMoreElements()) {
						Attribute  objAttrs = attrs.nextElement();
						_logger.trace("attribute "+objAttrs.getID() + " : " + objAttrs.get());
						attributeMap.put(objAttrs.getID().toLowerCase(), objAttrs);
					}
					
					Organizations organization = buildOrganization(attributeMap,sr.getName(),sr.getNameInNamespace());
					if(organization != null) {
						orgsList.add(organization);
						maxLevel = (maxLevel < organization.getLevel()) ? organization.getLevel() : maxLevel ;
					}
					
				}
			}
			
			for (int level = 2 ; level <= maxLevel ; level++) {
				for(Organizations organization : orgsList) {
					if(organization.getLevel() == level) {
						String parentNamePath= organization.getNamePath().substring(0, organization.getNamePath().lastIndexOf("/"));
						
						if(orgsNamePathMap.get(organization.getNamePath())!=null) {
						    _logger.info("org  " + orgsNamePathMap.get(organization.getNamePath()).getNamePath()+" exists.");
						    continue;
						}
						
						Organizations  parentOrg = orgsNamePathMap.get(parentNamePath);
						if(parentOrg == null ) {
							parentOrg = rootOrganization;
						}
						organization.setParentId(parentOrg.getId());
						organization.setParentName(parentOrg.getName());
						organization.setCodePath(parentOrg.getCodePath()+"/"+organization.getId());
						_logger.info("parentNamePath " + parentNamePath+" , namePah " + organization.getNamePath());
						
						organizationsService.saveOrUpdate(organization);
						orgsNamePathMap.put(organization.getNamePath(), organization);
						
						HistorySynchronizer historySynchronizer =new HistorySynchronizer();
			            historySynchronizer.setId(historySynchronizer.generateId());
			            historySynchronizer.setSyncId(this.synchronizer.getId());
			            historySynchronizer.setSyncName(this.synchronizer.getName());
			            historySynchronizer.setObjectId(organization.getId());
			            historySynchronizer.setObjectName(organization.getName());
			            historySynchronizer.setObjectType(Organizations.class.getSimpleName());
			            historySynchronizer.setInstId(synchronizer.getInstId());
			            historySynchronizer.setResult("success");
			           this.historySynchronizerService.insert(historySynchronizer);
					}
				}
			}
			
			//ldapUtils.close();
		} catch (NamingException e) {
			_logger.error("NamingException " , e);
		}
		
		
	}
	
	public Organizations buildOrganization(HashMap<String,Attribute> attributeMap,String name,String nameInNamespace) {
		try {
		    Organizations org = new Organizations();
			org.setLdapDn(nameInNamespace);
			nameInNamespace = nameInNamespace.replaceAll(",OU=", "/").replaceAll("OU=", "/");
			nameInNamespace = nameInNamespace.substring(0, nameInNamespace.length() - ldapUtils.getBaseDN().length() - 1);
			String []namePaths = nameInNamespace.split("/");
			String namePah= "/"+rootOrganization.getName();
			for(int i = namePaths.length -1 ; i >= 0 ; i --) {
			    namePah = namePah + "/" + namePaths[i];
			}
			
			namePah = namePah.substring(0, namePah.length() - 1);
			 
			org.setId(org.generateId());
			org.setCode(org.getId());
			org.setNamePath(namePah);
			org.setLevel(namePaths.length);
			org.setName(LdapUtils.getAttributeStringValue(OrganizationalUnit.OU,attributeMap));
			org.setCountry(LdapUtils.getAttributeStringValue(OrganizationalUnit.CO,attributeMap));
			org.setRegion(LdapUtils.getAttributeStringValue(OrganizationalUnit.ST,attributeMap));
			org.setLocality(LdapUtils.getAttributeStringValue(OrganizationalUnit.L,attributeMap));
			org.setStreet(LdapUtils.getAttributeStringValue(OrganizationalUnit.STREET,attributeMap));
			org.setPostalCode(LdapUtils.getAttributeStringValue(OrganizationalUnit.POSTALCODE,attributeMap));
			org.setDescription(LdapUtils.getAttributeStringValue(OrganizationalUnit.DESCRIPTION,attributeMap));
			org.setInstId(this.synchronizer.getInstId());
			org.setStatus(ConstsStatus.ACTIVE);
			
			_logger.debug("Organization " + org);
			return org;
		} catch (NamingException e) {
			_logger.error("NamingException " , e);
		}
		return null;
	}
	
	

	public ActiveDirectoryUtils getLdapUtils() {
		return ldapUtils;
	}

	public void setLdapUtils(ActiveDirectoryUtils ldapUtils) {
		this.ldapUtils = ldapUtils;
	}


}
