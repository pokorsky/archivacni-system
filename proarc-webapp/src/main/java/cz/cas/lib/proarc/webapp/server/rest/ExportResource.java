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

import com.yourmediashelf.fedora.generated.foxml.DigitalObject;
import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationException;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.export.DataStreamExport;
import cz.cas.lib.proarc.common.export.DesaExport;
import cz.cas.lib.proarc.common.export.DesaExport.Result;
import cz.cas.lib.proarc.common.export.ExportException;
import cz.cas.lib.proarc.common.export.ExportResultLog;
import cz.cas.lib.proarc.common.export.ExportResultLog.ResultError;
import cz.cas.lib.proarc.common.export.ExportUtils;
import cz.cas.lib.proarc.common.export.KWISExport;
import cz.cas.lib.proarc.common.export.Kramerius4Export;
import cz.cas.lib.proarc.common.export.archive.ArchiveOldPrintProducer;
import cz.cas.lib.proarc.common.export.archive.ArchiveProducer;
import cz.cas.lib.proarc.common.export.cejsh.CejshConfig;
import cz.cas.lib.proarc.common.export.cejsh.CejshExport;
import cz.cas.lib.proarc.common.export.cejsh.CejshStatusHandler;
import cz.cas.lib.proarc.common.export.crossref.CrossrefExport;
import cz.cas.lib.proarc.common.export.mets.MetsContext;
import cz.cas.lib.proarc.common.export.mets.MetsExportException;
import cz.cas.lib.proarc.common.export.mets.MetsExportException.MetsExportExceptionElement;
import cz.cas.lib.proarc.common.export.mets.MetsUtils;
import cz.cas.lib.proarc.common.export.mets.NdkExport;
import cz.cas.lib.proarc.common.export.mets.NdkSttExport;
import cz.cas.lib.proarc.common.export.mets.structure.IMetsElement;
import cz.cas.lib.proarc.common.export.mets.structure.MetsElement;
import cz.cas.lib.proarc.common.export.sip.NdkSipExport;
import cz.cas.lib.proarc.common.export.workflow.WorkflowExport;
import cz.cas.lib.proarc.common.fedora.DigitalObjectException;
import cz.cas.lib.proarc.common.fedora.FoxmlUtils;
import cz.cas.lib.proarc.common.fedora.RemoteStorage;
import cz.cas.lib.proarc.common.object.DigitalObjectManager;
import cz.cas.lib.proarc.common.object.model.MetaModelRepository;
import cz.cas.lib.proarc.common.user.UserProfile;
import cz.cas.lib.proarc.common.workflow.WorkflowActionHandler;
import cz.cas.lib.proarc.common.workflow.WorkflowException;
import cz.cas.lib.proarc.common.workflow.WorkflowManager;
import cz.cas.lib.proarc.common.workflow.model.Job;
import cz.cas.lib.proarc.common.workflow.model.Task;
import cz.cas.lib.proarc.common.workflow.model.TaskView;
import cz.cas.lib.proarc.common.workflow.profile.WorkflowDefinition;
import cz.cas.lib.proarc.common.workflow.profile.WorkflowProfiles;
import cz.cas.lib.proarc.webapp.shared.rest.ExportResourceApi;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.server.CloseableService;

/**
 * REST resource to export data from the system.
 *
 * @author Jan Pokorsky
 */
@Path(ExportResourceApi.PATH)
public class ExportResource {

    private final AppConfiguration appConfig;
    private final UserProfile user;
    private final SessionContext session;
    private HttpHeaders httpHeaders;

    public ExportResource(
            @Context SecurityContext securityCtx,
            @Context HttpServletRequest httpRequest
    ) throws AppConfigurationException {

        this.appConfig = AppConfigurationFactory.getInstance().defaultInstance();
        this.httpHeaders = httpHeaders;
        session = SessionContext.from(httpRequest);
        user = session.getUser();
    }

    public ExportResource(
            @Context SecurityContext securityCtx,
            @Context HttpServletRequest httpRequest,
            @Context HttpHeaders httpHeaders
            ) throws AppConfigurationException {

        this.appConfig = AppConfigurationFactory.getInstance().defaultInstance();
        this.httpHeaders = httpHeaders;
        session = SessionContext.from(httpRequest);
        user = session.getUser();
    }

    @POST
    @Path(ExportResourceApi.DATASTREAM_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> datastream(
            @FormParam(ExportResourceApi.DATASTREAM_PID_PARAM) List<String> pids,
            @FormParam(ExportResourceApi.DATASTREAM_DSID_PARAM) List<String> dsIds,
            @FormParam(ExportResourceApi.DATASTREAM_HIERARCHY_PARAM) @DefaultValue("true") boolean hierarchy
            ) throws IOException, ExportException {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.DATASTREAM_PID_PARAM);
        }
        if (dsIds.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.DATASTREAM_DSID_PARAM);
        }
        DataStreamExport export = new DataStreamExport(RemoteStorage.getInstance(appConfig), appConfig.getExportOptions());
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        File target = export.export(exportFolder, hierarchy, pids, dsIds);
        URI targetPath = user.getUserHomeUri().relativize(target.toURI());
        return new SmartGwtResponse<>(new ExportResult(targetPath));
    }

    @POST
    @Path(ExportResourceApi.KRAMERIUS4_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> kramerius4(
            @FormParam(ExportResourceApi.KRAMERIUS4_PID_PARAM) List<String> pids,
            @FormParam(ExportResourceApi.KRAMERIUS4_POLICY_PARAM) String policy,
            @FormParam(ExportResourceApi.KRAMERIUS4_HIERARCHY_PARAM) @DefaultValue("true") boolean hierarchy
            ) throws IOException {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.KRAMERIUS4_PID_PARAM);
        }
        Kramerius4Export export = new Kramerius4Export(
                RemoteStorage.getInstance(appConfig), appConfig, policy);
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        Kramerius4Export.Result k4Result = export.export(exportFolder, hierarchy, session.asFedoraLog(), pids.toArray(new String[pids.size()]));

        ExportResult result = null;
        if (k4Result.getValidationError() != null) {
            MetsUtils.renameFolder(exportFolder, k4Result.getFile(), null);
            result = new ExportResult(k4Result.getValidationError().getExceptions());
        } else {
            URI targetPath = user.getUserHomeUri().relativize(k4Result.getFile().toURI());
            result = new ExportResult(targetPath);

            for (String pid : pids) {
                try {
                    setWorkflowExport("task.exportK4", "param.exportK4", k4Result.getPageCount(), getRoot(pid, exportFolder));
                } catch (MetsExportException | DigitalObjectException | WorkflowException e) {
                    throw new IOException(e);
                }
            }
        }


        return new SmartGwtResponse<>(result);
    }

    /**
     * Starts a new export to DESA repository.
     *
     * @param pids PIDs to export
     * @param hierarchy export also children hierarchy of requested PIDs. Default is {@code false}.
     * @param forDownload export to file system for later client download. If {@code true} dryRun is ignored.
     *              Default is {@code false}.
     * @param dryRun use to build packages without sending to the repository. Default is {@code false}.
     * @return the list of results for requested PIDs
     * @throws IOException unexpected failure
     * @throws ExportException unexpected failure
     */
    @POST
    @Path(ExportResourceApi.DESA_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> newDesaExport(
            @FormParam(ExportResourceApi.DESA_PID_PARAM) List<String> pids,
            @FormParam(ExportResourceApi.DESA_HIERARCHY_PARAM) @DefaultValue("false") boolean hierarchy,
            @FormParam(ExportResourceApi.DESA_FORDOWNLOAD_PARAM) @DefaultValue("false") boolean forDownload,
            @FormParam(ExportResourceApi.DESA_DRYRUN_PARAM) @DefaultValue("false") boolean dryRun
            ) throws IOException, ExportException {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.DESA_PID_PARAM);
        }
        DesaExport export = new DesaExport(RemoteStorage.getInstance(appConfig),
                appConfig.getDesaServices(), MetaModelRepository.getInstance(), appConfig.getExportOptions());
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        List<ExportResult> result = new ArrayList<>(pids.size());
        if (forDownload) {
            Result r = export.exportDownload(exportFolder, pids.get(0));
            result.add(r.getValidationError() != null
                    ? new ExportResult(r.getValidationError().getExceptions())
                    : new ExportResult(r.getDownloadToken()));
        } else {
            if (dryRun) {
                for (String pid : pids) {
                    List<MetsExportExceptionElement> errors = export.validate(exportFolder, pid, hierarchy);
                    result.add(new ExportResult(errors));
                }
            } else {
                for (String pid : pids) {
                    Result r = export.export(exportFolder, pid, null, false, hierarchy, false, session.asFedoraLog(), user);
                    if (r.getValidationError() != null) {
                        result.add(new ExportResult(r.getValidationError().getExceptions()));
                    } else {
                        // XXX not used for now
                        result.add(new ExportResult((Integer) null, "done"));
                    }
                }
            }
        }
        return new SmartGwtResponse<>(result);
    }

    /**
     * Gets the exported package built by {@link #newDesaExport(List, boolean, boolean, boolean)}  } with {@code forDownload=true}.
     * The package data are removed after completion of the response.
     *
     * @param token token to identify the prepared package
     * @return the package contents in ZIP format
     */
    @GET
    @Path(ExportResourceApi.DESA_PATH)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getDesaExport(
            @QueryParam(ExportResourceApi.RESULT_TOKEN) String token,
            @Context CloseableService finalizer
            ) {

        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        final File file = DesaExport.findExportedPackage(exportFolder, token);
        if (file == null) {
            return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("The contents not found!").build();
        }
        finalizer.add(new Closeable() {

            @Override
            public void close() {
                FileUtils.deleteQuietly(file.getParentFile());
            }
        });
        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + '"')
                .build();
    }

    /**
     * Gets the exported package as PSP (default) or SIP (simpler format for eborn documents)
     * @param pids identifiers of saved objects in fedora repository
     * @param typeOfPackage (PSP | SIP]
     * @return ExportResult with path to package and possible errors from export
     * @throws ExportException
     */
    @POST
    @Path(ExportResourceApi.NDK_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> newNdkExport(
            @FormParam(ExportResourceApi.NDK_PID_PARAM) List<String> pids,
            @FormParam(ExportResourceApi.NDK_PACKAGE) @DefaultValue("PSP") String typeOfPackage
//            @FormParam(ExportResourceApi.DESA_HIERARCHY_PARAM) @DefaultValue("false") boolean hierarchy,
//            @FormParam(ExportResourceApi.DESA_FORDOWNLOAD_PARAM) @DefaultValue("false") boolean forDownload,
//            @FormParam(ExportResourceApi.DESA_DRYRUN_PARAM) @DefaultValue("false") boolean dryRun
            ) throws ExportException {
        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.DESA_PID_PARAM);
        }
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        List<ExportResult> result = new ArrayList<>(pids.size());
        NdkExport export;

        switch (typeOfPackage) {
            case "PSP":
                export = new NdkExport(RemoteStorage.getInstance(), appConfig);
                break;
            case "SIP":
                export = new NdkSipExport(RemoteStorage.getInstance(), appConfig);
                break;
            case "STT":
                export = new NdkSttExport(RemoteStorage.getInstance(), appConfig);
                break;
            case "CHRONICLE":
                export = new NdkExport(RemoteStorage.getInstance(), appConfig);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of package");
        }

        List<NdkExport.Result> ndkResults = export.export(exportFolder, pids, true, true, null, session.asFedoraLog());
        for (NdkExport.Result r : ndkResults) {
            if (r.getValidationError() != null) {
                if (isMissingURNNBN(r) && appConfig.getExportOptions().isDeletePackage()) {
                    MetsUtils.deleteFolder(r.getTargetFolder());
                } else {
                    MetsUtils.renameFolder(exportFolder, r.getTargetFolder(), null);
                }
                result.add(new ExportResult(r.getValidationError().getExceptions()));
            } else {
                // XXX not used for now
                result.add(new ExportResult((Integer) null, "done"));
            }
        }
        if ("done".equals(result.get(0).getTarget())) {
            for (NdkExport.Result r : ndkResults) {
                try {
                    setWorkflowExport("task.exportNdkPsp", "param.exportNdkPsp.numberOfPackages", r.getPageIndexCount(), getRoot(r.getPid(), exportFolder));
                } catch (MetsExportException | DigitalObjectException | WorkflowException e) {
                    result.clear();
                    result.add(new ExportResult(null, "Vyexportovano ale nepodarilo se propojit s RDflow."));
                }
            }
        }

        return new SmartGwtResponse<>(result);
    }

    private IMetsElement getRoot(String pid, File exportFolder) throws MetsExportException {
            RemoteStorage rstorage = RemoteStorage.getInstance();
            RemoteStorage.RemoteObject fo = rstorage.find(pid);
            MetsContext mc = buildContext(rstorage, fo, null, exportFolder);
            return getMetsElement(fo, mc, true);
    }

    private MetsElement getMetsElement(RemoteStorage.RemoteObject fo, MetsContext dc, boolean hierarchy) throws MetsExportException {
        dc.resetContext();
        DigitalObject dobj = MetsUtils.readFoXML(fo.getPid(), fo.getClient());
        if (dobj == null) {
            throw new MetsExportException("Missing uuid");
        }
        return MetsElement.getElement(dobj, null, dc, hierarchy);
    }

    private MetsContext buildContext(RemoteStorage rstorage, RemoteStorage.RemoteObject fo, String packageId, File targetFolder) {
        MetsContext mc = new MetsContext();
        mc.setFedoraClient(fo.getClient());
        mc.setRemoteStorage(rstorage);
        mc.setPackageID(packageId);
        mc.setOutputPath(targetFolder.getAbsolutePath());
        mc.setAllowNonCompleteStreams(false);
        mc.setAllowMissingURNNBN(false);
        mc.setConfig(appConfig.getNdkExportOptions());
        return mc;
    }

    private void setWorkflowExport(String taskName, String parameterName, Integer pageCount, IMetsElement root) throws DigitalObjectException, WorkflowException {
       if (root != null) {
            DigitalObjectManager dom = DigitalObjectManager.getDefault();
            DigitalObjectManager.CreateHandler handler = dom.create(root.getModel(), root.getOriginalPid(), null, user, null, session.asFedoraLog());
            Locale locale = session.getLocale(httpHeaders);
            Job job = handler.getWfJob(root.getOriginalPid(), locale);
            if (job == null) {
                return;
            }
            List<TaskView> tasks = handler.getTask(job.getId(), locale);
            Task editedTask = null;
            for (TaskView task : tasks) {
                if (taskName.equals(task.getTypeRef())) {
                    editedTask = task;
                    break;
                }
            }
            if (editedTask != null) {
                editedTask.setOwnerId(new BigDecimal(session.getUser().getId()));
                editedTask.setState(Task.State.FINISHED);
                WorkflowProfiles workflowProfiles = WorkflowProfiles.getInstance();
                WorkflowDefinition workflow = workflowProfiles.getProfiles();
                WorkflowManager workflowManager = WorkflowManager.getInstance();

                try {
                /*    TaskFilter taskFilter = new TaskFilter();
                    taskFilter.setId(editedTask.getId());
                    taskFilter.setLocale(locale);
                    Task.State previousState = workflowManager.tasks().findTask(taskFilter, workflow).stream()
                            .findFirst().get().getState();
                 */   if (parameterName != null) {
                        Map<String, Object> parameters = new HashMap<>();
                        parameters.put(parameterName, pageCount);
                        workflowManager.tasks().updateTask(editedTask, parameters, workflow);
                    } else {
                        workflowManager.tasks().updateTask(editedTask, (Map<String, Object>) null, workflow);
                    }
                   // List<TaskView> result = workflowManager.tasks().findTask(taskFilter, workflow);

                    //if (result != null && !result.isEmpty() && result.get(0).getState() != previousState) {
                        WorkflowActionHandler workflowActionHandler = new WorkflowActionHandler(workflow, locale);
                        workflowActionHandler.runAction(editedTask);
                    //}
                } catch (IOException e) {
                    throw new DigitalObjectException(e.getMessage());
                }
            }
        }
    }

    /**
     * @return true if at least one exception contains "URNNBN misssing" otherwise @return false
     */
    private boolean isMissingURNNBN(NdkExport.Result r) {
        for (MetsExportExceptionElement exception : r.getValidationError().getExceptions()) {
            if ("URNNBN identifier is missing".equals(exception.getMessage())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts a new CEJSH export.
     * @param pids PIDs to export
     * @return the export result
     */
    @POST
    @Path(ExportResourceApi.CEJSH_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> newCejshExport(
            @FormParam(ExportResourceApi.CEJSH_PID_PARAM) List<String> pids
            ) {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.CEJSH_PID_PARAM);
        }
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        CejshConfig cejshConfig = CejshConfig.from(appConfig.getAuthenticators());
        CejshExport export = new CejshExport(
                DigitalObjectManager.getDefault(), RemoteStorage.getInstance(), appConfig);
        CejshStatusHandler status = export.export(exportFolder, pids);
        File targetFolder = status.getTargetFolder();
        ExportResult result = new ExportResult();
        if (targetFolder != null) {
            result.setTarget(user.getUserHomeUri().relativize(targetFolder.toURI()).toASCIIString());
        }
        if (!status.isOk()) {
            result.setErrors(new ArrayList<>());
            for (ExportResultLog.ExportResult logResult : status.getReslog().getExports()) {
                for (ResultError error : logResult.getError()) {
                    result.getErrors().add(new ExportError(
                            error.getPid(), error.getMessage(), false, error.getDetails()));
                }
            }
            MetsUtils.renameFolder(exportFolder, targetFolder, null);
        }
        return new SmartGwtResponse<>(result);
    }

    /**
     * Starts a new Crossref export.
     * @param pids PIDs to export
     * @return the export result
     */
    @POST
    @Path(ExportResourceApi.CROSSREF_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> newCrossrefExport(
            @FormParam(ExportResourceApi.CROSSREF_PID_PARAM) List<String> pids
            ) {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.CROSSREF_PID_PARAM);
        }
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
//        CejshConfig cejshConfig = CejshConfig.from(appConfig.getAuthenticators());
        CrossrefExport export = new CrossrefExport(
                DigitalObjectManager.getDefault(), RemoteStorage.getInstance(), appConfig.getExportOptions());
        CejshStatusHandler status = new CejshStatusHandler();
        export.export(exportFolder, pids, status);
        File targetFolder = status.getTargetFolder();
        ExportResult result = new ExportResult();
        if (targetFolder != null) {
            result.setTarget(user.getUserHomeUri().relativize(targetFolder.toURI()).toASCIIString());
        }
        if (!status.isOk()) {
            result.setErrors(new ArrayList<>());
            for (ExportResultLog.ExportResult logResult : status.getReslog().getExports()) {
                for (ResultError error : logResult.getError()) {
                    result.getErrors().add(new ExportError(
                            error.getPid(), error.getMessage(), false, error.getDetails()));
                }
            }
            MetsUtils.renameFolder(exportFolder, targetFolder, null);
        }
        return new SmartGwtResponse<>(result);
    }

    /**
     * Starts new archiving.
     * @param pids PIDs to export
     * @return the export result
     */
    @POST
    @Path(ExportResourceApi.ARCHIVE_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> newArchive(
            @FormParam(ExportResourceApi.ARCHIVE_PID_PARAM) List<String> pids,
            @FormParam(ExportResourceApi.NDK_PACKAGE) @DefaultValue("PSP") String typeOfPackage
            ) throws ExportException {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.ARCHIVE_PID_PARAM);
        }
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        ExportResult result = new ExportResult();
        List<ExportResult> resultList = new ArrayList<>();
        ArchiveProducer export = null;
        switch (typeOfPackage) {
            case "PSP":
                export = new ArchiveProducer(appConfig);;
                break;
            case "STT":
                export = new ArchiveOldPrintProducer(appConfig);;
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of package");
        }

        File targetFolder = ExportUtils.createFolder(exportFolder, "archive_" + FoxmlUtils.pidAsUuid(pids.get(0)), appConfig.getExportOptions().isOverwritePackage());
        try {
            //File archiveRootFolder = ExportUtils.createFolder(targetFolder, "archive_" + FoxmlUtils.pidAsUuid(pids.get(0)));
            targetFolder = export.archive(pids, targetFolder);
            if (targetFolder != null) {
                result.setTarget(user.getUserHomeUri().relativize(targetFolder.toURI()).toASCIIString());
            }
        } catch (Exception ex) {
            Logger.getLogger(ExportResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        ExportResultLog reslog = export.getResultLog();
        result.setErrors(new ArrayList<>());
        for (ExportResultLog.ExportResult logResult : reslog.getExports()) {
            for (ResultError error : logResult.getError()) {
                if (isMissingURNNBN(error) && appConfig.getExportOptions().isDeletePackage()) {
                   MetsUtils.deleteFolder(targetFolder);
                   error.setDetails(null);
                } else {
                    MetsUtils.renameFolder(exportFolder, targetFolder, null);
                }
                result.getErrors().add(new ExportError(
                        error.getPid(), error.getMessage(), false, error.getDetails()));
            }
        }
        resultList.add(result);
        if (!result.getErrors().isEmpty()) {
            return new SmartGwtResponse<>(resultList);
        }
        NdkExport exportNdk;
        switch (typeOfPackage) {
            case "PSP":
                exportNdk = new NdkExport(RemoteStorage.getInstance(), appConfig);
                break;
            case "STT":
                exportNdk = new NdkSttExport(RemoteStorage.getInstance(), appConfig);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of package");
        }
        File target = null;
        for (File file : targetFolder.listFiles()) {
            if (file.isDirectory()) {
                target = ExportUtils.createFolder(file, "NDK", false);
                continue;
            }
        }
        if (target == null) {
            target = targetFolder;
        }
        List<NdkExport.Result> ndkResults = exportNdk.exportNdkArchive(targetFolder, pids, true, true, true, session.asFedoraLog());
        for (NdkExport.Result r : ndkResults) {
            if (r.getValidationError() != null) {
                if (isMissingURNNBN(r) && appConfig.getExportOptions().isDeletePackage()) {
                    //MetsUtils.deleteFolder(r.getTargetFolder());
                } else {
                    MetsUtils.renameFolder(exportFolder, targetFolder, target);
                }
                resultList.add(new ExportResult(r.getValidationError().getExceptions()));
            } else {
                // XXX not used for now
                resultList.add(new ExportResult((Integer) null, "done"));
            }
        }
        boolean errors = false;
        for (ExportResource.ExportResult log: resultList) {
            if (log.getErrors() == null || log.getErrors().isEmpty()) {
                errors = false;
            } else {
                errors = true;
                break;
            }
        }

        if (!errors) {
            ExportUtils.writeExportResult(targetFolder, export.getResultLog());
            for (NdkExport.Result r : ndkResults) {
                try {
                    setWorkflowExport("task.exportArchive", "param.exportArchive", r.getPageIndexCount(), getRoot(r.getPid(), exportFolder));
                } catch (MetsExportException | DigitalObjectException e) {
                    resultList.clear();
                    resultList.clear();
                    resultList.add(new ExportResult(null, "Vyexportovano ale nepodarilo se propojit s RDflow."));
                } catch (WorkflowException e) {
                    if ("Task is blocked by other tasks!".equals(e.getMessage())) {
                        String targetLog = resultList.get(0).getTarget() + ", ale zapsan\u00ed \u00fakolu je blokov\u00e1no!";
                        resultList.get(0).setTarget(targetLog);
                    } else {
                        resultList.clear();
                        resultList.add(new ExportResult(null, "Vyexportovano ale nepodarilo se propojit s RDflow."));
                    }
                }
            }

            WorkflowExport exportWorkflow = new WorkflowExport(appConfig, user, session.getLocale(httpHeaders));
            try {
                exportWorkflow.export(targetFolder, ndkResults, session.asFedoraLog());
            } catch (Exception ex ) {
                resultList.clear();
                resultList.add(new ExportResult(null, "Nepodarilo se vytvorit soubor workflow_onformation.xml"));
            }
        }

        return new SmartGwtResponse<>(resultList);
    }

    private boolean isMissingURNNBN(ResultError error) {
        return "URNNBN identifier is missing".equals(error.getMessage());
    }

    @POST
    @Path(ExportResourceApi.KWIS_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ExportResult> newKwisExport(
            @FormParam(ExportResourceApi.KWIS_PID_PARAM) List<String> pids,
            @FormParam(ExportResourceApi.KRAMERIUS4_POLICY_PARAM) String policy,
            @FormParam(ExportResourceApi.KWIS_HIERARCHY_PARAM) @DefaultValue("true") boolean hierarchy
    ) throws IOException, ExportException {

        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.KRAMERIUS4_PID_PARAM);
        }
        ExportResult result = new ExportResult();

        URI imagesPath = runDatastreamExport(pids, Collections.singletonList("NDK_USER"), hierarchy);
        URI k4Path = runK4Export(pids, hierarchy, policy, "public");

        String outputPath = user.getExportFolder().getPath();
        String imp = imagesPath.getPath();
        String k4p = k4Path.getPath();
        String exportPackPath = outputPath + pids.get(0).substring(5) + "_KWIS";
        File exportFolder = new File(exportPackPath);
        exportFolder.mkdir();

        imp = outputPath + imp.substring(imp.indexOf('/') + 1);
        k4p = outputPath + k4p.substring(k4p.indexOf('/') + 1);

        KWISExport export = new KWISExport(
                appConfig,
                imp,
                k4p,
                exportPackPath);

        try {
            export.run();
        } catch (Exception ex) {
            result.setErrors(new ArrayList<>());
            result.getErrors().add(new ExportError(pids.get(0), ex.getMessage(), false, "Not configurated KWIS export."));
            MetsUtils.renameFolder(new File(outputPath), exportFolder, null);
        } finally {
            FileUtils.deleteDirectory(new File(imp));
            FileUtils.deleteDirectory(new File(k4p));
        }
        return new SmartGwtResponse<>(result);
    }

    private URI runK4Export(List<String> pids, boolean hierarchy, String policy, String exportPageContext) throws IOException {
        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.KRAMERIUS4_PID_PARAM);
        }

        Kramerius4Export export = new Kramerius4Export(RemoteStorage.getInstance(appConfig), appConfig, policy);
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        Kramerius4Export.Result target = export.export(exportFolder, hierarchy, session.asFedoraLog(), pids.toArray(new String[pids.size()]));
        return user.getUserHomeUri().relativize(target.getFile().toURI());
    }

    private URI runDatastreamExport(
            List<String> pids,
            List<String> dsIds,
            boolean hierarchy
    ) throws IOException, ExportException {
        if (pids.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.DATASTREAM_PID_PARAM);
        }
        if (dsIds.isEmpty()) {
            throw RestException.plainText(Status.BAD_REQUEST, "Missing " + ExportResourceApi.DATASTREAM_DSID_PARAM);
        }
        DataStreamExport export = new DataStreamExport(RemoteStorage.getInstance(appConfig), appConfig.getExportOptions());
        URI exportUri = user.getExportFolder();
        File exportFolder = new File(exportUri);
        File target = export.export(exportFolder, hierarchy, pids, dsIds);
        return user.getUserHomeUri().relativize(target.toURI());
    }

    @GET
    @Path("alephexport")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> alephExportState() {
        File[] listOfFiles = new File("/tmp/aleph").listFiles();

        if (listOfFiles == null) {
            return new ArrayList<>();
        }

        List<String> fileNames = new ArrayList<>();

        for (File file : listOfFiles) {
            fileNames.add(file.getName());
        }

        return fileNames;
    }


    /**
     * The export result.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ExportResult {

        @XmlElement(name = ExportResourceApi.RESULT_ID)
        private Integer exportId;

        @XmlElement(name = ExportResourceApi.RESULT_TOKEN)
        private String token;

        /**
         * The target folder path.
         */
        @XmlElement(name = ExportResourceApi.RESULT_TARGET)
        private String target;

        @XmlElement(name = ExportResourceApi.RESULT_ERRORS)
        private List<ExportError> errors;

        public ExportResult() {
        }

        public ExportResult(URI targetPath) {
            this.target = targetPath.toASCIIString();
        }

        public ExportResult(String token) {
            this.token = token;
        }

        public ExportResult(Integer exportId, String target) {
            this.exportId = exportId;
            this.target = target;
        }

        public ExportResult(List<MetsExportExceptionElement> validations) {
            if (validations != null) {
                errors = new ArrayList<>();
                for (MetsExportExceptionElement me : validations) {
                    errors.add(new ExportError(me));
                }
            }
        }

        public Integer getExportId() {
            return exportId;
        }

        public String getTarget() {
            return target;
        }

        /**
         * Muset be mutable {@link ExportResource#newArchive(List)}
         * @return
         */
        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        public List<ExportError> getErrors() {
            if (errors == null) {
                errors = new ArrayList<>();
            }
            return errors;
        }

        public void setExportId(Integer exportId) {
            this.exportId = exportId;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public void setErrors(List<ExportError> errors) {
            this.errors = new ArrayList<>(errors);
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

    }

    /**
     * The export error.
     */
    @XmlRootElement(name = ExportResourceApi.RESULT_ERROR)
    public static class ExportError {

        @XmlElement(name = ExportResourceApi.RESULT_ERROR_PID)
        private String pid;

        @XmlElement(name = ExportResourceApi.RESULT_ERROR_MESSAGE)
        private String message;

        @XmlElement(name = ExportResourceApi.RESULT_ERROR_WARNING)
        private boolean warning;

        @XmlElement(name = ExportResourceApi.RESULT_ERROR_LOG)
        private String log;

        public ExportError() {
        }

        public ExportError(String pid, String message, boolean warning, String log) {
            this.pid = pid;
            this.message = message;
            this.warning = warning;
            this.log = log;
        }

        public ExportError(MetsExportExceptionElement me) {
            this.pid = me.getPid();
            this.message = me.getMessage();
            this.warning = me.isWarning();
            List<String> validations = me.getValidationErrors();
            Exception ex = me.getEx();
            if (validations != null && !validations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String validation : validations) {
                    if (message == null) {
                        message = validation;
                    }
                    sb.append(validation).append('\n');
                }
                this.log = sb.toString();
            } else if (ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                pw.close();
                this.log = sw.toString();
            }
        }

    }

}
