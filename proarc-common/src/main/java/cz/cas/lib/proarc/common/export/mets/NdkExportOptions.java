/*
 * Copyright (C) 2017 Lukas Sykora
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
package cz.cas.lib.proarc.common.export.mets;

import org.apache.commons.configuration.Configuration;

/**
 * Settings for NDK export
 *
 * @author lsykora
 */
public class NdkExportOptions {
    static final String PROP_NDK_AGENT_ARCHIVIST = "export.ndk.agent.archivist";
    static final String PROP_NDK_AGENT_CREATOR = "export.ndk.agent.creator";
    static final String PROP_DELETE_PACKAGE = "export.ndk.deletePackageIfUrnNbnIsMissing";
    private String archivist;
    private String creator;
    private boolean deletePackage;

    public static NdkExportOptions getOptions(Configuration config) {
        NdkExportOptions options = new NdkExportOptions();

        String creator = config.getString(PROP_NDK_AGENT_CREATOR);
        if (creator != null && !creator.isEmpty()) {
            options.setCreator(creator);
        }

        String archivist = config.getString(PROP_NDK_AGENT_ARCHIVIST);
        if (archivist != null && !archivist.isEmpty()) {
            options.setArchivist(archivist);
        }

        String deletePackage = config.getString(PROP_DELETE_PACKAGE);
        options.setDeletePackage(Boolean.parseBoolean(deletePackage));

        return options;
    }

    /** Returns the creator organization - used for mets header */
    public String getCreator() {
        return creator;
    }

    /** Sets the creator organization*/
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /** Returns the archivist organization - used for mets header */
    public String getArchivist() {
        return archivist;
    }

    /** Sets the archivist organization */
    public void setArchivist(String archivist) {
        this.archivist = archivist;
    }

    /** Returns value of deleting package - used for unsuccesfull export */
    public boolean isDeletePackage() {
        return deletePackage;
    }

    /** Sets value of deleting package */
    public void setDeletePackage(boolean deletePackage) {
        this.deletePackage = deletePackage;
    }
}
