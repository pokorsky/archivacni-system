/*
 * Copyright (C) 2013 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.authentication.desa;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.cas.lib.proarc.authentication.Authenticator;
import cz.cas.lib.proarc.authentication.ProarcPrincipal;
import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationException;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.process.export.desa.DesaServices;
import cz.cas.lib.proarc.common.process.export.desa.DesaServices.DesaConfiguration;
import cz.cas.lib.proarc.common.user.Group;
import cz.cas.lib.proarc.common.user.Permissions;
import cz.cas.lib.proarc.common.user.UserManager;
import cz.cas.lib.proarc.common.user.UserProfile;
import cz.cas.lib.proarc.common.user.UserUtil;
import cz.cas.lib.proarc.desa.DesaClient;
import cz.cas.lib.proarc.desa.soap.AuthenticateUserFault;
import cz.cas.lib.proarc.desa.soap.AuthenticateUserResponse;
import cz.cas.lib.proarc.desa.soap.Role;

/**
 * DESA authentication.
 * It authenticates credentials in all registered DESA services. At least one
 * authentication response must contain required role to pass.
 * @author pavels
 */
public class DESAAuthenticator implements Authenticator {
    
    public static Logger LOGGER = Logger.getLogger(DESAAuthenticator.class.getName());
    
    public static final String KOD_PUVODCE = "kod";
    public static final String REMOTE_TYPE = DesaServices.REMOTE_USER_TYPE;
    public static final String USER_PREFIX = "desa";
    private static final String ROLE = "producer_submit";

    public DESAAuthenticator() {
    }

    /**
     * @return the authenticated user or {@code null}
     */
    private UserProfile authenticateDesaUser(String tUser, String tPass, String code) {
        try {
            AppConfiguration appConfig = AppConfigurationFactory.getInstance().defaultInstance();
            DesaServices desaServices = appConfig.getDesaServices();
            List<DesaConfiguration> configurations = desaServices.getConfigurations();
            AuthenticateUserResponse authorizedUser = null;
            for (DesaConfiguration desConf : configurations) {
                if (authorizedUser != null && Boolean.getBoolean("desa.oneAuthIsEnough")) {
                    break;
                }
                DesaClient client = desaServices.getDesaClient(desConf);
                AuthenticateUserResponse desaUser = client.authenticateUser(tUser, tPass, code);
                if (desaUser == null) {
                    return null;
                }
                if (isAuthorized(desaUser)) {
                    authorizedUser = desaUser;
                }
            }
            if (authorizedUser != null) {
                return createLocalUser(authorizedUser, tUser, code);
            }
        } catch (AppConfigurationException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
            throw new IllegalStateException("Cannot initialize configuration! See server log.");
        } catch (AuthenticateUserFault e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return null;
    }

    private UserProfile createLocalUser(AuthenticateUserResponse desaUser, String desaUserName, String producerCode) {
        UserManager userManger = UserUtil.getDefaultManger();
        Group remoteGroup = userManger.findRemoteGroup(producerCode, REMOTE_TYPE);
        if (remoteGroup == null) {
            remoteGroup = Group.createRemote(
                    UserUtil.toUserName(USER_PREFIX, producerCode),
                    "DESA " + producerCode,
                    producerCode, REMOTE_TYPE);
            userManger.addGroup(remoteGroup, Arrays.asList(Permissions.REPO_SEARCH_GROUPOWNER),
                    "proarc", "Add remote DESA group.");
        }
        UserProfile proarcUser = userManger.find(desaUserName, REMOTE_TYPE);
        if (proarcUser == null) {
            String surname = isNullString(desaUser.getSurname()) ? desaUserName : desaUser.getSurname();
            proarcUser = UserProfile.createRemote(desaUserName, REMOTE_TYPE, surname);
            proarcUser.setUserName(UserUtil.toUserName(USER_PREFIX, desaUserName));
            proarcUser.setEmail(desaUser.getEmail());
            proarcUser.setForename(desaUser.getName());
            // set default ownership
            proarcUser.setDefaultGroup(remoteGroup.getId());
            userManger.add(proarcUser, Arrays.asList(remoteGroup), "proarc", "Add remote DESA user.");
        }
        return proarcUser;
    }

    boolean isAuthorized(AuthenticateUserResponse desaUser) {
        if (desaUser != null && desaUser.getRoles() != null) {
            for (Role role : desaUser.getRoles().getItem()) {
                if (ROLE.equals(role.getRoleAcr())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AuthenticatedState authenticate(Map<String, String> loginProperties,
            HttpServletRequest request, HttpServletResponse response,
            ProarcPrincipal principal) {

        String user = loginProperties.get(LOGINNAME);
        String pswd = loginProperties.get(PASSWORD);
        String kod = loginProperties.get(KOD_PUVODCE);
        if (isNullString(kod)) return AuthenticatedState.IGNORED;
        if (isNullString(user) || isNullString(pswd)) {
            return AuthenticatedState.FORBIDDEN;
        }
        UserProfile authenticated = authenticateDesaUser(user, pswd, kod);
        if (authenticated != null) {
            principal.associateUserProfile(authenticated);
        }
        
        return authenticated != null ? AuthenticatedState.AUTHENTICATED : AuthenticatedState.FORBIDDEN;
    }

    boolean isNullString(String str) {
        return str == null || str.trim().equals("");
    }

}
