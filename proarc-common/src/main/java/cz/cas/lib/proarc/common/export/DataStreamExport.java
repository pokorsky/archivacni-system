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
package cz.cas.lib.proarc.common.export;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import com.yourmediashelf.fedora.client.response.ListDatastreamsResponse;
import com.yourmediashelf.fedora.generated.access.DatastreamType;
import cz.cas.lib.proarc.common.export.mets.MetsExportException;
import cz.cas.lib.proarc.common.export.mets.MimeType;
import cz.cas.lib.proarc.common.fedora.DigitalObjectException;
import cz.cas.lib.proarc.common.fedora.FoxmlUtils;
import cz.cas.lib.proarc.common.fedora.RemoteStorage;
import cz.cas.lib.proarc.common.fedora.RemoteStorage.RemoteObject;
import cz.cas.lib.proarc.common.fedora.relation.RelationEditor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports particular data streams of queried digital objects.
 * It can traverse hierarchy of digital objects.
 *
 * @author Jan Pokorsky
 */
public final class DataStreamExport {

    private static final Logger LOG = Logger.getLogger(DataStreamExport.class.getName());

    private RemoteStorage rstorage;
    /** already exported PIDs to prevent loops */
    private HashSet<String> exportedPids = new HashSet<String>();
    /** PIDs scheduled for export */
    private Queue<String> toExport = new LinkedList<String>();
    private byte[] buffer = new byte[10*1024];
    private final ExportOptions options;

    public DataStreamExport(RemoteStorage rstorage, ExportOptions options) {
        this.rstorage = rstorage;
        this.options = options;
    }

    public File export(File output, boolean hierarchy, List<String> pids, List<String> dsIds) throws ExportException {
        if (!output.exists() || !output.isDirectory()) {
            throw new IllegalStateException(String.valueOf(output));
        }
        if (pids == null || pids.isEmpty() || dsIds == null || dsIds.isEmpty()) {
            throw new IllegalArgumentException();
        }

        File target = ExportUtils.createFolder(output, filename(pids.get(0), dsIds.get(0)), options.isOverwritePackage());
        toExport.addAll(pids);
        for (String pid = toExport.poll(); pid != null; pid = toExport.poll()) {
            exportPid(target, hierarchy, pid, dsIds);
        }
        return target;
    }

    private void exportPid(File target, boolean hierarchy, String pid, List<String> dsIds) throws ExportException {
        String extension;
        if (exportedPids.contains(pid)) {
            return ;
        }
        exportedPids.add(pid);

        RemoteObject remote = rstorage.find(pid);
        
        try {
            extension = getExtension(dsIds, getDataStreams(remote));
            dsIds = filterDataStreams(dsIds, getDataStreams(remote));
        } catch (FedoraClientException ex) {
            throw new ExportException(pid, ex);
        }

        if (hierarchy) {
            try {
                RelationEditor relationEditor = new RelationEditor(remote);
                List<String> members = relationEditor.getMembers();
                toExport.addAll(members);
            } catch (DigitalObjectException ex) {
                throw new ExportException(ex);
            }
        }

        for (String dsId : dsIds) {
            try {
                exportPid(target, remote, dsId, extension);
            } catch (FedoraClientException ex) {
                throw new ExportException(filename(pid, dsId), ex);
            } catch (IOException ex) {
                throw new ExportException(filename(pid, dsId), ex);
            } catch (MetsExportException ex) {
                throw new ExportException(filename(pid, dsId), ex);
            }
        }

    }

    private void exportPid(File target, RemoteObject remote, String dsId, String extension) throws FedoraClientException, IOException, MetsExportException {
        InputStream input = getDataStreamDissemination(remote, dsId);
        File f = new File(target, filename(remote.getPid(), MimeType.getExtension(extension)));
        boolean done = false;
        try {
            FileOutputStream output = new FileOutputStream(f);
            try {
                copy(input, output);
                done = true;
            } finally {
                try {
                    output.close();
                } catch (IOException ex) {
                    if (done) {
                        throw ex;
                    } else {
                        LOG.log(Level.SEVERE, f.toString(), ex);
                    }
                }
            }
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                if (done) {
                    throw ex;
                } else {
                    LOG.log(Level.SEVERE, f.toString(), ex);
                }
            }
        }
    }

    private InputStream getDataStreamDissemination(RemoteObject remote, String dsId) throws FedoraClientException {
        FedoraResponse response = FedoraClient.getDatastreamDissemination(remote.getPid(), dsId)
                .execute(remote.getClient());
        InputStream input = response.getEntityInputStream();
        return input;
    }

    private List<DatastreamType> getDataStreams(RemoteObject remote) throws FedoraClientException {
        ListDatastreamsResponse response = FedoraClient.listDatastreams(remote.getPid())
                .execute(remote.getClient());
        return response.getDatastreams();
    }

    /**
     * FedoraClient.getDatastreamDissemination throws HTTP 404 in case of
     * undefined streams.
     */
    private List<String> filterDataStreams(List<String> dsId, List<DatastreamType> streams) {
        ArrayList<String> result = new ArrayList<String>(dsId.size());
        for (DatastreamType stream : streams) {
            if (dsId.contains(stream.getDsid())) {
                result.add(stream.getDsid());
                if (dsId.size() == result.size()) {
                    break;
                }
            }
        }
        return result;
    }

    private String getExtension(List<String> dsId, List<DatastreamType> streams) {
        for (DatastreamType stream : streams) {
            if (dsId.contains(stream.getDsid())) {
                return stream.getMimeType();
            }
        }
        return "";
    }

    private void copy(InputStream is, OutputStream os) throws IOException {
        for (int length = 0; (length = is.read(buffer)) > 0; ) {
            os.write(buffer, 0, length);
        }
        os.close();
    }

    static String filename(String pid, String dsId) {
        return FoxmlUtils.pidAsUuid(pid) + '.' + dsId;
    }

}
