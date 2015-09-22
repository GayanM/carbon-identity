/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.provider.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.identity.provisioning.dao.ProvisioningManagementDAO;
import org.wso2.carbon.identity.scim.common.utils.AttributeMapper;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.charon.core.attributes.SimpleAttribute;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.NotFoundException;
import org.wso2.charon.core.objects.AbstractSCIMObject;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ProvisioningEntityBuilder {

    private static volatile ProvisioningEntityBuilder provisioningEntityBuilder = null;
    private ProvisioningManagementDAO provisioningManagementDAO = null;
    private static Log log = LogFactory.getLog(ProvisioningEntityBuilder.class);

    private ProvisioningEntityBuilder(){
        provisioningManagementDAO = new ProvisioningManagementDAO();
    }

    public static ProvisioningEntityBuilder getInstance() {
        if (provisioningEntityBuilder == null) {
            synchronized (ProvisioningEntityBuilder.class) {
                provisioningEntityBuilder = new ProvisioningEntityBuilder();
            }
        }
        return provisioningEntityBuilder;
    }

    ProvisioningEntity getProvisioningEntityForUserAdd(SCIMObject provisioningObject,
                                                               Map<ClaimMapping, List<String>> outboundAttributes,
                                                               String domainName) throws CharonException {
        User user = (User)provisioningObject;
        if (user.getPassword() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                           IdentityProvisioningConstants.PASSWORD_CLAIM_URI, null, null, false),
                                   Arrays.asList(new String[] { user.getPassword() }));
        }

        if (user.getUserName() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                           .build(IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                                   Arrays.asList(new String[] { user.getUserName() }));
        }

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { user.getId() }));

        String domainAwareName =
                UserCoreUtil.addDomainToName(user.getUserName(), domainName);
        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.POST,
                                       outboundAttributes);
        Map<String, String> inboundAttributes =
                AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);
        provisioningEntity.setInboundAttributes(inboundAttributes);

        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForUserDelete(SCIMObject provisioningObject,
                                                                  Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                  String domainName) throws CharonException, IdentityApplicationManagementException {
        User user = (User) provisioningObject;
        String username = provisioningManagementDAO.getProvisionedEntityNameByLocalId(user.getId());
        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false), Arrays
                                       .asList(new String[] { username }));
        if (log.isDebugEnabled()) {
            log.debug("Adding domain name : " + domainName + " to user SCIM ID : " + user.getId());
        }
        String domainAwareName = UserCoreUtil.addDomainToName(user.getId(), domainName);

        ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.DELETE,
                outboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForUserUpdate(SCIMObject provisioningObject,
                                                                  Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                  String domainName) throws CharonException, IdentityApplicationManagementException {
        User user = (User) provisioningObject;
        if (user.getUserName() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                           IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null,
                                           false),
                                   Arrays.asList(new String[] { user.getUserName() }));
        }

        String domainAwareName =
                UserCoreUtil.addDomainToName(user.getUserName(), domainName);
        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.PUT,
                                       outboundAttributes);
        Map<String, String> inboundAttributes =
                AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);
        provisioningEntity.setInboundAttributes(inboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForUserPatch(SCIMObject provisioningObject,
                                                                 Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                 String domainName) throws CharonException, IdentityApplicationManagementException {
        User user = (User) provisioningObject;
        if (user.getUserName() == null) {
            user.setDisplayName(provisioningManagementDAO.getProvisionedEntityNameByLocalId(user.getId()));
        }

        if (user.getUserName() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                           IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null,false),
                                   Arrays.asList(new String[] { user.getUserName() }));
        }

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { user.getId() }));

        String domainAwareName =
                UserCoreUtil.addDomainToName(user.getUserName(), domainName);
        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.PATCH,
                                       outboundAttributes);
        Map<String, String> inboundAttributes = AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);
        provisioningEntity.setInboundAttributes(inboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForGroupAdd(SCIMObject provisioningObject,
                                                                Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {
        Group group = (Group) provisioningObject;
        if (provisioningObject.getAttribute("displayName") != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                    IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays.asList(
                    new String[] { ((SimpleAttribute) provisioningObject.getAttribute("displayName"))
                                           .getStringValue() }));
        }
        List<String> userList = group.getMembersWithDisplayName();

        if (!userList.isEmpty()) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                    IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false), userList);
        }

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { group.getId() }));

        if (log.isDebugEnabled()) {
            log.debug("Adding domain name : " + domainName + " to role : " + group.getDisplayName());
        }
        String domainAwareName = UserCoreUtil.addDomainToName(group.getDisplayName(), domainName);

        ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.POST,
                outboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForGroupDelete(SCIMObject provisioningObject,
                                                                   Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                   String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {
        Group group = (Group) provisioningObject;
        String roleName = provisioningManagementDAO.getProvisionedEntityNameByLocalId(group.getId());
        if (roleName != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                    IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                                           .asList(new String[] { roleName }));
        }
        String domainAwareName = UserCoreUtil.addDomainToName(roleName, domainName);
        ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.DELETE, outboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForGroupUpdate(SCIMObject provisioningObject,
                                                                   Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                   String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {
        Group group = (Group) provisioningObject;
        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                                       .asList(new String[] { group.getDisplayName() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                       .build(IdentityProvisioningConstants.USERNAME_CLAIM_URI,
                                              null, null, false), group.getMembersWithDisplayName());

        String domainAwareName = UserCoreUtil.addDomainToName(group.getDisplayName(), domainName);

        String oldGroupName = provisioningManagementDAO.getProvisionedEntityNameByLocalId(group.getId());

        ProvisioningEntity provisioningEntity = null;
        if (!oldGroupName.equals(group.getDisplayName())) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                           .build(IdentityProvisioningConstants.OLD_GROUP_NAME_CLAIM_URI,
                                                  null, null, false), Arrays.asList(new String[] { oldGroupName }));
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                           .build(IdentityProvisioningConstants.NEW_GROUP_NAME_CLAIM_URI,
                                                  null, null, false), Arrays.asList(new String[] { domainAwareName }));
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                           IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                                   Arrays.asList(new String[] { group.getId() }));
            domainAwareName = UserCoreUtil.addDomainToName(oldGroupName, domainName);
            provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.PUT, outboundAttributes);
        } else {
            provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.PUT, outboundAttributes);
        }
        return provisioningEntity;
    }

    ProvisioningEntity getProvisioningEntityForGroupPatch(SCIMObject provisioningObject,
                                                                  Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
                                                                  String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {
        Group group = (Group) provisioningObject;
        if (group.getDisplayName() == null) {
            group.setDisplayName(provisioningManagementDAO.getProvisionedEntityNameByLocalId(group.getId()));
        }
        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                       .build(IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { group.getDisplayName() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                               group.getMembersWithDisplayName());

        String domainAwareName = UserCoreUtil.addDomainToName(group.getDisplayName(), domainName);

        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.PUT,
                                       outboundAttributes);
        return provisioningEntity;
    }
}