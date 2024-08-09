/*
 * Copyright (C) 2015 Jan Pokorsky
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
package cz.cas.lib.proarc.webapp.shared.rest;

import cz.cas.lib.proarc.webapp.server.rest.v1.WorkflowResourceV1;

/**
 * Constants for {@link WorkflowResourceV1}
 * shared by GWT client.
 *
 * @author Jan Pokorsky
 */
public class WorkflowResourceApi {

    public static final String PATH = "workflow";

    public static final String NEWJOB_PROFILE = "profileName";
    public static final String NEWJOB_MODEL = "model";
    public static final String NEWJOB_METADATA = "metadata";
    public static final String NEWJOB_CATALOGID = "catalogId";
    public static final String NEWJOB_PARENTID = "parentId";

    public static final String MATERIAL_PATH = "material";

    public static final String PARAMETER_PATH = "parameter";

    public static final String PROFILE_PATH = "profile";

    public static final String TASK_PATH = "task";

    public static final String MODS_PATH = "mods";
    public static final String MODS_PLAIN_PATH = "plain";

    public static final String NEWJOB_RDCZID = "rdczId";
    public static final String EDITOR_JOBS = "editorJobs";
}
