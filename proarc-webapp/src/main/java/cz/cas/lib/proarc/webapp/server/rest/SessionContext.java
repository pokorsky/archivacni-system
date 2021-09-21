/*
 * Copyright (C) 2012 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.server.rest;

import cz.cas.lib.proarc.authentication.ProarcPrincipal;
import cz.cas.lib.proarc.common.user.Permission;
import cz.cas.lib.proarc.common.user.UserManager;
import cz.cas.lib.proarc.common.user.UserProfile;
import cz.cas.lib.proarc.common.user.UserUtil;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

/**
 * Information about current request session.
 * 
 * @author Jan Pokorsky
 */
public final class SessionContext {

    private UserProfile user;
    private String ip;

    public SessionContext(UserProfile user, String ip) {
        this.user = user;
        this.ip = ip;
    }

    public static SessionContext from(HttpServletRequest request) throws WebApplicationException{
        Principal userPrincipal = request.getUserPrincipal();
        String remoteAddr = getRemoteAddr(request);
        if (userPrincipal != null && userPrincipal instanceof ProarcPrincipal) {
            ProarcPrincipal proarcPrincipal = (ProarcPrincipal) userPrincipal;
            UserProfile user = proarcPrincipal.getAssociatedUserProfile();
            if (user != null) {
                return new SessionContext(user, remoteAddr);
            }
        }
        throw new WebApplicationException(Status.FORBIDDEN);
    }

    public static SessionContext from(UserProfile user, String clientIp) {
        return new SessionContext(user, clientIp);
    }

    /**
     * Gets session context as Fedora log in JSON format.
     * @return
     */
    public String asFedoraLog() {
        // use Jackson for complex JSON; String.format is enough for now
        return String.format("{\"proarc\":{\"user\":\"%s\",\"ip\":\"%s\"}}", user.getUserName(), ip);
    }

    public Locale getLocale(HttpHeaders httpHeaders) {
        List<Locale> acceptableLanguages = httpHeaders.getAcceptableLanguages();
        Locale locale = acceptableLanguages.isEmpty() ? Locale.ENGLISH : acceptableLanguages.get(0);
        return locale;
    }

    public UserProfile getUser() {
        return user;
    }

    public String getIp() {
        return ip;
    }

    public boolean checkPermission(Permission... permissions) {
        UserManager userManager = UserUtil.getDefaultManger();
        Set<Permission> grants = userManager.findUserPermissions(user.getId());
        return grants.containsAll(Arrays.asList(permissions));
    }

    public boolean checkRole(String requiredRole) {
        UserManager userManager = UserUtil.getDefaultManger();
        String role = userManager.findUserRole(user.getId());
        if (role != null) {
            return requiredRole.equals(role);
        }
        return false;
    }

    public void requirePermission(String role, Permission... permissions) {
        if (!checkPermission(permissions)  && !checkRole(role)) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    /**
     * Guesses real client IP. E.g. Apache with mod_proxy in front of Tomcat
     * puts origin client IP to X-Forwarded-For header.
     * @param request
     * @return
     */
    private static String getRemoteAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
