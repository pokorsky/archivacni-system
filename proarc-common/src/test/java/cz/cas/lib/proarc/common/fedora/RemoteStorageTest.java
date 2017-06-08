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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.common.fedora;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.response.DescribeRepositoryResponse;
import com.yourmediashelf.fedora.client.response.ListDatastreamsResponse;
import com.yourmediashelf.fedora.generated.access.DatastreamType;
import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import cz.cas.lib.proarc.common.CustomTemporaryFolder;
import cz.cas.lib.proarc.common.dublincore.DcStreamEditor;
import cz.cas.lib.proarc.common.fedora.FoxmlUtils.ControlGroup;
import cz.cas.lib.proarc.common.fedora.LocalStorage.LocalObject;
import cz.cas.lib.proarc.common.fedora.RemoteStorage.RemoteObject;
import cz.cas.lib.proarc.common.fedora.RemoteStorage.RemoteXmlStreamEditor;
import cz.cas.lib.proarc.common.fedora.XmlStreamEditor.EditorResult;
import cz.cas.lib.proarc.common.fedora.relation.RelationEditor;
import cz.cas.lib.proarc.common.mods.ModsStreamEditor;
import cz.cas.lib.proarc.mods.ModsDefinition;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Jan Pokorsky
 */
public class RemoteStorageTest {
    
    private static FedoraClient client;
    private static FedoraTestSupport support;

    @Rule
    public TestName test = new TestName();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public CustomTemporaryFolder tmp = new CustomTemporaryFolder();

    public RemoteStorageTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        support = new FedoraTestSupport();
        client = support.getClient();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        support.cleanUp();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testVersion() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        DescribeRepositoryResponse descr = fedora.getRepositoryDescription(true);
        assertNotNull(descr);
        String version = descr.getRepositoryVersion();
        assertNotNull(version);
        assertTrue(version, fedora.isCompatible("3.5"));
    }

    @Test
    public void testIngest() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        LocalObject object = new LocalStorage().create();
        String label = "testing";
        object.setLabel(label);
        fedora.ingest(object, "junit", "{\"key\":\"val\"}");
    }

    @Test
    public void testIngestPage() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
//        client.debug(true);
        LocalObject local = new LocalStorage().create();
//        LocalObject local = new LocalStorage().create(new File("/tmp/failing_ingest.foxml"));
        String model = "model:page";

        prepareLocalObject(local, model);
//        System.out.println(FoxmlUtils.toXml(local.getDigitalObject(), true));

        String label = "testing";
        local.setLabel(label);
        fedora.ingest(local, "junit");
        ListDatastreamsResponse response = FedoraClient.listDatastreams(local.getPid()).execute(client);
        List<DatastreamType> datastreams = response.getDatastreams();
        assertDatastream(DcStreamEditor.DATASTREAM_ID, datastreams);
        assertDatastream(ModsStreamEditor.DATASTREAM_ID, datastreams);
        assertDatastream(RelationEditor.DATASTREAM_ID, datastreams);
        assertDatastream(StringEditor.OCR_ID, datastreams);
        assertDatastream(BinaryEditor.THUMB_ID, datastreams);
    }

    @Test
    public void testPurge() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        LocalObject object = new LocalStorage().create();
        String label = "testPurge";
        object.setLabel(label);
        fedora.ingest(object, "junit");
        assertTrue(object.getPid(), fedora.exist(object.getPid()));
        RemoteObject robject = fedora.find(object.getPid());
        robject.purge("{\"key\":\"val\"}");
        assertFalse(object.getPid(), fedora.exist(object.getPid()));
    }

    @Test
    public void testPurgeMissing() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        RemoteObject missing = fedora.find("uuid:missing");
        thrown.expect(DigitalObjectNotFoundException.class);
        missing.purge("testPurgeMissing");
    }

    private static void assertDatastream(String id, List<DatastreamType> datastreams) {
        for (DatastreamType ds : datastreams) {
            if (id.equals(ds.getDsid())) {
                return;
            }
        }
        fail(id);
    }

    @Test
    public void testDatastreamPurge() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
//        client.debug(true);
        LocalObject local = new LocalStorage().create();
//        LocalObject local = new LocalStorage().create(new File("/tmp/failing_ingest.foxml"));
        String model = "model:page";

        prepareLocalObject(local, model);

        String label = "testing";
        local.setLabel(label);
        fedora.ingest(local, "junit");
        ListDatastreamsResponse response = FedoraClient.listDatastreams(local.getPid()).execute(client);
        List<DatastreamType> datastreams = response.getDatastreams();
        assertDatastream(DcStreamEditor.DATASTREAM_ID, datastreams);
        assertDatastream(ModsStreamEditor.DATASTREAM_ID, datastreams);
        assertDatastream(RelationEditor.DATASTREAM_ID, datastreams);
        assertDatastream(StringEditor.OCR_ID, datastreams);
        assertDatastream(BinaryEditor.THUMB_ID, datastreams);

        RemoteObject robject = fedora.find(local.getPid());
        robject.purgeDatastream(BinaryEditor.THUMB_ID, "{\"key\":\"val\"}");
        response = FedoraClient.listDatastreams(local.getPid()).execute(client);
        datastreams = response.getDatastreams();

        assertFalse(datastreams.contains(BinaryEditor.THUMB_ID));
    }

    private void prepareLocalObject(LocalObject local, String model) throws DigitalObjectException, IOException {
        RelationEditor relsExt = new RelationEditor(local);
        relsExt.setModel(model);
        relsExt.write(0, null);
        ModsStreamEditor modsEditor = new ModsStreamEditor(local);
        ModsDefinition mods = modsEditor.createPage(local.getPid(), "1", "[1]", "Blank");
        DcStreamEditor dcEditor = new DcStreamEditor(local);
        dcEditor.write(mods, model, 0, null);
        modsEditor.write(mods, 0, null);

        StringEditor ocrEditor = StringEditor.ocr(local);
        ocrEditor.write("ocr", 0, null);

        File thumb = tmp.newFile();
        assertTrue(thumb.exists());
        BinaryEditor.dissemination(local, BinaryEditor.THUMB_ID).write(thumb, 0, null);
        local.flush();
//        System.out.println(FoxmlUtils.toXml(local.getDigitalObject(), true));
    }

    @Test
    public void testXmlRead() throws Exception {
        String dsId = "testId";
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        String format = "testns";
        XmlStreamEditor leditor = local.getEditor(FoxmlUtils.inlineProfile(dsId, format, "label"));
        EditorResult editorResult = leditor.createResult();
        TestXml content = new TestXml("test content");
        JAXB.marshal(content, editorResult);
        leditor.write(editorResult, 0, null);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, "junit");

        RemoteObject remote = fedora.find(local.getPid());
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote, dsId);
        Source src = editor.read();
        assertNotNull(src);
        TestXml resultContent = JAXB.unmarshal(src, TestXml.class);
        assertEquals(content, resultContent);
        long lastModified = editor.getLastModified();
        assertTrue(String.valueOf(lastModified), lastModified != 0 && lastModified < System.currentTimeMillis());
        assertEquals(format, editor.getProfile().getDsFormatURI());
    }

    @Test(expected = DigitalObjectNotFoundException.class)
    public void testXmlReadDataStreamOfMissingPid() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        // no ingest!
        RemoteObject remote = fedora.find(local.getPid());
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote,
                FoxmlUtils.managedProfile("managedDatastream",
                        MediaType.TEXT_PLAIN_TYPE, "managedDatastreamLabel"));
        editor.read();
    }

    /**
     * test read missing data stream with
     * {@link RemoteXmlStreamEditor#RemoteXmlStreamEditor(RemoteObject, String) }.
     */
    @Test(expected = DigitalObjectException.class)
    public void testReadMissingDataStreamFailure() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        fedora.ingest(local, "junit");
        RemoteObject remote = fedora.find(local.getPid());
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote, "missingDatastream");
        editor.read();
    }

    @Test
    public void testPlainTextReadWriteMissingDataStream() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        fedora.ingest(local, "junit");
        RemoteObject remote = fedora.find(local.getPid());
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote,
                FoxmlUtils.managedProfile("managedDatastream",
                        MediaType.TEXT_PLAIN_TYPE, "managedDatastreamLabel"));
        InputStream src = editor.readStream();
        assertNull(src);
        long lastModified = editor.getLastModified();
        assertEquals(-1, lastModified);
        assertEquals(MediaType.TEXT_PLAIN, editor.getProfile().getDsMIME());
        editor.write("plain text".getBytes("UTF-8"), lastModified, null);
        remote.flush();
        
        // first test current editor
        src = editor.readStream();
        assertNotNull(src);
        assertEquals(MediaType.TEXT_PLAIN, editor.getProfile().getDsMIME());
        assertTrue(lastModified < editor.getLastModified());
        String content = StringEditor.read(src);
        assertEquals("plain text", content);
        assertNull(editor.getProfile().getDsFormatURI());

        // test new editor
        editor = new RemoteXmlStreamEditor(remote,
            FoxmlUtils.managedProfile("managedDatastream",
                    MediaType.TEXT_PLAIN_TYPE, "managedDatastreamLabel"));
        src = editor.readStream();
        assertNotNull(src);
        assertEquals(MediaType.TEXT_PLAIN, editor.getProfile().getDsMIME());
        assertTrue(lastModified < editor.getLastModified());
        content = StringEditor.read(src);
        assertEquals("plain text", content);
        assertNull(editor.getProfile().getDsFormatURI());
}

    @Test
    public void testXmlWrite() throws Exception {
        String dsId = "testId";
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        String format = "testns";
        XmlStreamEditor leditor = local.getEditor(FoxmlUtils.inlineProfile(dsId, format, "label"));
        EditorResult editorResult = leditor.createResult();
        TestXml content = new TestXml("test content");
        JAXB.marshal(content, editorResult);
        leditor.write(editorResult, 0, null);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, "junit");

        RemoteObject remote = fedora.find(local.getPid());
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote, dsId);
        Source src = editor.read();
        assertNotNull(src);
        TestXml resultContent = JAXB.unmarshal(src, TestXml.class);

        // write modification
        String expectedContent = "changed test content";
        resultContent.data = expectedContent;
        editorResult = editor.createResult();
        JAXB.marshal(resultContent, editorResult);
        long lastModified = editor.getLastModified();
        assertEquals(format, editor.getProfile().getDsFormatURI());
        editor.write(editorResult, lastModified, null);
        remote.flush();

        // test current editor
        assertTrue(lastModified < editor.getLastModified());
        long expectLastModified = editor.getLastModified();
        resultContent = JAXB.unmarshal(editor.read(), TestXml.class);
        assertEquals(new TestXml(expectedContent), resultContent);
        assertEquals(format, editor.getProfile().getDsFormatURI());

        // test new editor
        remote = fedora.find(local.getPid());
        editor = new RemoteXmlStreamEditor(remote, dsId);
        src = editor.read();
        assertNotNull(src);
        resultContent = JAXB.unmarshal(src, TestXml.class);
        assertEquals(new TestXml(expectedContent), resultContent);
        long resultLastModified = editor.getLastModified();
        assertEquals(expectLastModified, resultLastModified);
        assertEquals(format, editor.getProfile().getDsFormatURI());
    }

    @Test(expected = DigitalObjectConcurrentModificationException.class)
    public void testXmlWriteConcurrent() throws Exception {
        String dsId = "testId";
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        XmlStreamEditor leditor = local.getEditor(FoxmlUtils.inlineProfile(dsId, "testns", "label"));
        EditorResult editorResult = leditor.createResult();
        TestXml content = new TestXml("test content");
        JAXB.marshal(content, editorResult);
        leditor.write(editorResult, 0, null);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, "junit");

        RemoteObject remote = fedora.find(local.getPid());
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote, dsId);
        Source src = editor.read();
        assertNotNull(src);
        TestXml resultContent = JAXB.unmarshal(src, TestXml.class);

        // concurrent write
        RemoteObject concurrentRemote = fedora.find(local.getPid());
        RemoteXmlStreamEditor concurrentEditor = new RemoteXmlStreamEditor(concurrentRemote, dsId);
        TestXml concurrentContent = JAXB.unmarshal(concurrentEditor.read(), TestXml.class);
        concurrentContent.data = "concurrent change";
        EditorResult concurrentResult = concurrentEditor.createResult();
        JAXB.marshal(concurrentContent, concurrentResult);
        concurrentEditor.write(concurrentResult, editor.getLastModified(), null);
        concurrentRemote.flush();

        // write out of date modification
        String expectedContent = "changed test content";
        resultContent.data = expectedContent;
        editorResult = editor.createResult();
        JAXB.marshal(resultContent, editorResult);
        long lastModified = editor.getLastModified();
        editor.write(editorResult, lastModified, null);

        remote.flush();
    }

    @Test
    public void testDatastreamEditorWriteStream_Managed() throws Exception {
        LocalStorage storage = new LocalStorage();
        LocalObject local = storage.create();
        local.setLabel(test.getMethodName());
        String dsID = "dsID";
        MediaType mime = MediaType.TEXT_PLAIN_TYPE;
        String label = "label";
        DatastreamProfile dsProfile = FoxmlUtils.managedProfile(dsID, mime, label);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, "junit");

        byte[] data = "data".getBytes("UTF-8");
        RemoteObject remote = fedora.find(local.getPid());
        XmlStreamEditor reditor = remote.getEditor(dsProfile);
        assertNotNull(reditor);
        reditor.write(new ByteArrayInputStream(data), reditor.getLastModified(), null);

        // test read cached
        InputStream is = reditor.readStream();
        assertNotNull(is);
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data, resultData.toByteArray());
        remote.flush();

        // test remote read
        remote = fedora.find(local.getPid());
        reditor = remote.getEditor(dsProfile);
        is = reditor.readStream();
        assertNotNull(is);
        resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data, resultData.toByteArray());
    }

    @Test
    public void testDatastreamEditorWriteReference_Managed() throws Exception {
        LocalStorage storage = new LocalStorage();
        LocalObject local = storage.create();
        local.setLabel(test.getMethodName());
        String dsID = "dsID";
        MediaType mime = MediaType.TEXT_PLAIN_TYPE;
        String label = "label";
        DatastreamProfile dsProfile = FoxmlUtils.managedProfile(dsID, mime, label);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, "junit");

        // prepare referenced contents
        byte[] data = "data".getBytes("UTF-8");
        File file = tmp.newFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();

        RemoteObject remote = fedora.find(local.getPid());
        XmlStreamEditor reditor = remote.getEditor(dsProfile);
        assertNotNull(reditor);
        reditor.write(file.toURI(), reditor.getLastModified(), null);

        // test read cached
        InputStream is = reditor.readStream();
        assertNotNull(is);
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data, resultData.toByteArray());
        remote.flush();

        // test remote read
        remote = fedora.find(local.getPid());
        reditor = remote.getEditor(dsProfile);
        is = reditor.readStream();
        assertNotNull(is);
        resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data, resultData.toByteArray());
    }

    @Test
    public void testDatastreamEditorWriteReference_External() throws Exception {
        LocalStorage storage = new LocalStorage();
        LocalObject local = storage.create();
        local.setLabel(test.getMethodName());
        String dsID = "dsID";
        MediaType mime = MediaType.TEXT_PLAIN_TYPE;
        String label = "label";
        DatastreamProfile dsProfile = FoxmlUtils.externalProfile(dsID, mime, label);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, support.getTestUser());

        // prepare referenced contents
        byte[] data = "data".getBytes("UTF-8");
        File file = tmp.newFile();
        FileUtils.writeByteArrayToFile(file, data);

        RemoteObject remote = fedora.find(local.getPid());
        XmlStreamEditor reditor = remote.getEditor(dsProfile);
        assertNotNull(reditor);
        reditor.write(file.toURI(), reditor.getLastModified(), "add");

        // test read cached
        InputStream is = reditor.readStream();
        assertNotNull(is);
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data, resultData.toByteArray());
        assertEquals(mime.toString(), reditor.getProfile().getDsMIME());
        assertEquals(ControlGroup.EXTERNAL.toExternal(), reditor.getProfile().getDsControlGroup());

        // test remote read
        remote.flush();
        remote = fedora.find(local.getPid());
        reditor = remote.getEditor(dsProfile);
        is = reditor.readStream();
        assertNotNull(is);
        resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data, resultData.toByteArray());
        assertEquals(mime.toString(), reditor.getProfile().getDsMIME());
        assertEquals(ControlGroup.EXTERNAL.toExternal(), reditor.getProfile().getDsControlGroup());

        // test update MIME + location
        MediaType mime2 = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        byte[] data2 = "data2".getBytes("UTF-8");
        File file2 = tmp.newFile();
        FileUtils.writeByteArrayToFile(file2, data2);
        remote = fedora.find(local.getPid());
        reditor = remote.getEditor(dsProfile);
        DatastreamProfile dsProfile2 = FoxmlUtils.externalProfile(dsID, mime2, label);
        reditor.setProfile(dsProfile2);
        reditor.write(file2.toURI(), reditor.getLastModified(), "update");
        remote.flush();

        remote = fedora.find(local.getPid());
        reditor = remote.getEditor(dsProfile);
        is = reditor.readStream();
        assertNotNull(is);
        resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data2, resultData.toByteArray());
        assertEquals(mime2.toString(), reditor.getProfile().getDsMIME());
        assertEquals(ControlGroup.EXTERNAL.toExternal(), reditor.getProfile().getDsControlGroup());
    }

    @Test
    public void testDatastreamEditorRewriteControlGroup() throws Exception {
        // prepare referenced contents
        byte[] data1 = "data1".getBytes("UTF-8");
        File file1 = tmp.newFile();
        FileUtils.writeByteArrayToFile(file1, data1);
        byte[] data2 = "data2".getBytes("UTF-8");
        File file2 = tmp.newFile();
        FileUtils.writeByteArrayToFile(file2, data2);

        LocalStorage storage = new LocalStorage();
        LocalObject local = storage.create();
        System.out.println(local.getPid());
        local.setLabel(test.getMethodName());
        String dsID = "dsID";
        String label = "label";
        MediaType mime1 = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        DatastreamProfile dsProfile1 = FoxmlUtils.managedProfile(dsID, mime1, label);
        XmlStreamEditor leditor = local.getEditor(dsProfile1);
        assertNotNull(leditor);
        leditor.write(file1.toURI(), leditor.getLastModified(), null);
        local.flush();

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, support.getTestUser());

        MediaType mime2 = MediaType.TEXT_PLAIN_TYPE;
        DatastreamProfile dsProfile2 = FoxmlUtils.externalProfile(dsID, mime2, label);
        RemoteObject remote = fedora.find(local.getPid());
        XmlStreamEditor reditor = remote.getEditor(dsProfile1);
        assertNotNull(reditor);
        reditor.setProfile(dsProfile2);
        reditor.write(file2.toURI(), reditor.getLastModified(), null);

        // test read cached
        InputStream is = reditor.readStream();
        assertNotNull(is);
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data2, resultData.toByteArray());
        assertEquals(mime2.toString(), reditor.getProfile().getDsMIME());
        assertEquals(ControlGroup.EXTERNAL.toExternal(), reditor.getProfile().getDsControlGroup());

        // test remote read
        remote.flush();
        remote = fedora.find(local.getPid());
        reditor = remote.getEditor(dsProfile1);
        is = reditor.readStream();
        assertNotNull(is);
        resultData = new ByteArrayOutputStream();
        FoxmlUtils.copy(is, resultData);
        is.close();
        assertArrayEquals(data2, resultData.toByteArray());
        assertEquals(mime2.toString(), reditor.getProfile().getDsMIME());
        assertEquals(ControlGroup.EXTERNAL.toExternal(), reditor.getProfile().getDsControlGroup());
    }

    @Test
    public void testSetDatastreamProfile() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        fedora.ingest(local, "junit");
        RemoteObject remote = fedora.find(local.getPid());
        String dsId = "missingDatastream";
        MediaType mime = MediaType.TEXT_PLAIN_TYPE;

        // first test missing datastream
        RemoteXmlStreamEditor editor = new RemoteXmlStreamEditor(remote, FoxmlUtils.managedProfile(dsId, mime, "defaultLabel"));
        DatastreamProfile profile = editor.getProfile();
        assertEquals(mime.toString(), profile.getDsMIME());
        String expectedLabel = "label1";
        profile.setDsLabel(expectedLabel);
        editor.setProfile(profile);
        editor.write(new byte[2], editor.getLastModified(), "write1");
        remote.flush();

        editor = new RemoteXmlStreamEditor(remote, FoxmlUtils.managedProfile(dsId, mime, ""));
        profile = editor.getProfile();
        assertEquals(mime.toString(), profile.getDsMIME());
        assertEquals(expectedLabel, profile.getDsLabel());

        // test existing datastream
        expectedLabel = "label2";
        MediaType newMime = MediaType.TEXT_HTML_TYPE;
        profile = editor.getProfile();
        profile.setDsMIME(newMime.toString());
        profile.setDsLabel(expectedLabel);
        editor.setProfile(profile);
        editor.write(new byte[2], editor.getLastModified(), "write2");
        profile = editor.getProfile();
        assertEquals(newMime.toString(), profile.getDsMIME());
        assertEquals(expectedLabel, profile.getDsLabel());
        remote.flush();

        editor = new RemoteXmlStreamEditor(remote, FoxmlUtils.managedProfile(dsId, mime, ""));
        profile = editor.getProfile();
        assertEquals(newMime.toString(), profile.getDsMIME());
        assertEquals(expectedLabel, profile.getDsLabel());

        // test standalone profile change (without content)
        newMime = MediaType.APPLICATION_JSON_TYPE;
        expectedLabel = "label3";
        profile.setDsMIME(newMime.toString());
        profile.setDsLabel(expectedLabel);
        editor.setProfile(profile);
        remote.flush();

        editor = new RemoteXmlStreamEditor(remote, FoxmlUtils.managedProfile(dsId, mime, ""));
        profile = editor.getProfile();
        assertEquals(newMime.toString(), profile.getDsMIME());
        assertEquals(expectedLabel, profile.getDsLabel());
    }

    @Test
    public void testGetStreamProfile() throws Exception {
        String dsId = "testId";
        LocalObject local = new LocalStorage().create();
        local.setLabel(test.getMethodName());
        String format = "testns";
        XmlStreamEditor leditor = local.getEditor(FoxmlUtils.inlineProfile(dsId, format, "label"));
        EditorResult editorResult = leditor.createResult();
        TestXml content = new TestXml("test content");
        JAXB.marshal(content, editorResult);
        leditor.write(editorResult, 0, null);

        RemoteStorage fedora = new RemoteStorage(client);
        fedora.ingest(local, support.getTestUser());

        RemoteObject remote = fedora.find(local.getPid());
        List<DatastreamProfile> resultProfiles = remote.getStreamProfile(null);
        assertNotNull(resultProfiles);
        assertEquals(2, resultProfiles.size()); // + DC
        assertEquals(dsId, resultProfiles.get(0).getDsID());
    }

    @Test
    public void testObjectNotFound() throws Exception {
        RemoteStorage fedora = new RemoteStorage(client);
        RemoteObject remote = fedora.find("prefix:unknown");
        try {
            remote.asText();
        } catch (DigitalObjectNotFoundException ex) {
            // ok
        }
    }

    @XmlRootElement(namespace="testns")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TestXml {

        String data;

        public TestXml() {
        }

        public TestXml(String data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestXml other = (TestXml) obj;
            if ((this.data == null) ? (other.data != null) : !this.data.equals(other.data)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "TestXml{" + "data=" + data + '}';
        }

    }
}
