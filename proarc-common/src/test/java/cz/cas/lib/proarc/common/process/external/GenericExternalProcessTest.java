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
package cz.cas.lib.proarc.common.process.external;

import cz.cas.lib.proarc.common.CustomTemporaryFolder;
import cz.cas.lib.proarc.common.process.imports.InputUtils;
import cz.cas.lib.proarc.common.process.imports.TiffImporterTest;
import cz.cas.lib.proarc.common.process.external.GenericExternalProcess.ParamHandler;
import cz.cas.lib.proarc.common.process.external.GenericExternalProcess.ProcessResult;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Jan Pokorsky
 */
public class GenericExternalProcessTest {

    @Rule
    public CustomTemporaryFolder temp = new CustomTemporaryFolder(true);

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testIMConvert() throws Exception {
//        temp.setDeleteOnExit(false);
        String imageMagicExec = "/usr/bin/convert";
        Assume.assumeTrue(new File(imageMagicExec).exists());

        File confFile = temp.newFile("props.cfg");
        File root = temp.getRoot();
        URL pdfaResource = TiffImporterTest.class.getResource("pdfa_test.pdf");
        File pdfa = new File(root, "pdfa_test.pdf");
        FileUtils.copyURLToFile(pdfaResource, pdfa);
        FileUtils.writeLines(confFile, Arrays.asList(
                "input.file.name=RESOLVED",
                "exec=" + imageMagicExec,
//                "arg=-verbose",
                "arg=-thumbnail",
                "arg=120x128",
                "arg=$${input.file}[0]",
                "arg=-flatten",
                "arg=$${output.file}",
                "id=test"
        ));
        PropertiesConfiguration conf = new PropertiesConfiguration(confFile);
        GenericExternalProcess gep = new GenericExternalProcess(conf);
        gep.addInputFile(pdfa);
        File output = new File(root, "pdfa.jpg");
        gep.addOutputFile(output);
        gep.run();
//        System.out.printf("#exit: %s, out: %s\nresults: %s\n",
//                gep.getExitCode(), gep.getFullOutput(), gep.getResultParameters());
        assertEquals("exit code", 0, gep.getExitCode());
        assertTrue(output.toString(), output.exists());
        assertTrue("Not JPEG", InputUtils.isJpeg(output));
    }

    /** onExit is an experimental feature .*/
    @Test
    public void testIMConvertOnExit() throws Exception {
        String imageMagicExec = "/usr/bin/convert";
        Assume.assumeTrue(new File(imageMagicExec).exists());
        File confFile = temp.newFile("props.cfg");
        File root = temp.getRoot();
        URL pdfaResource = TiffImporterTest.class.getResource("pdfa_test.pdf");
        File pdfa = new File(root, "pdfa_test.pdf");
        FileUtils.copyURLToFile(pdfaResource, pdfa);
        FileUtils.writeLines(confFile, Arrays.asList(
                "input.file.name=RESOLVED",
                "exec=" + imageMagicExec,
                "arg=-thumbnail",
                "arg=120x128",
                "arg=$${input.file}[0]",
                "arg=-flatten",
                "arg=$${input.folder}/$${input.file.name}.jpg",
                "result.file=$${input.folder}/$${input.file.name}.jpg",
                "onExits=0",
                "onExit.0.param.output.file=$${input.folder}/$${input.file.name}.jpg",
                "id=test"
        ));
        PropertiesConfiguration conf = new PropertiesConfiguration(confFile);
        GenericExternalProcess gep = new GenericExternalProcess(conf);
        gep.addInputFile(pdfa);
        gep.run();
//        System.out.printf("#exit: %s, out: %s\nresults: %s\n",
//                gep.getExitCode(), gep.getFullOutput(), gep.getResultParameters());
        assertEquals("exit code", 0, gep.getExitCode());
        File output = gep.getOutputFile();
        assertNotNull(output);
        assertTrue(output.toString(), output.exists());
        assertTrue("Not JPEG", InputUtils.isJpeg(output));
    }

    @Test
    public void testIMConvertFailure() throws Exception {
        String imageMagicExec = "/usr/bin/convert";
        Assume.assumeTrue(new File(imageMagicExec).exists());
        File confFile = temp.newFile("props.cfg");
        File root = temp.getRoot();
        File pdfa = new File(root, "pdfa_test.pdf");
        FileUtils.writeLines(confFile, Arrays.asList(
                "input.file.name=RESOLVED",
                "exec=" + imageMagicExec,
                "arg=-thumbnail",
                "arg=120x128",
                "arg=$${input.file}[0]",
                "arg=-flatten",
                "arg=$${input.folder}/$${input.file.name}.jpg",
                "onExits=0",
                "onExit.0.param.output.file=$${input.folder}/$${input.file.name}.jpg",
                "id=test"
        ));
        PropertiesConfiguration conf = new PropertiesConfiguration(confFile);
        GenericExternalProcess gep = new GenericExternalProcess(conf);
        gep.addInputFile(pdfa);
        gep.run();
//        System.out.printf("#exit: %s, out: %s\nresults: %s\n",
//                gep.getExitCode(), gep.getFullOutput(), gep.getResultParameters());
        assertEquals("exit code", 1, gep.getExitCode());
        String outputPath = gep.getResultParameters().get("test.param." + GenericExternalProcess.DST_PATH);
        assertNull(outputPath);
    }

    @Test
    public void testEscapePropertiesConfiguration() throws Exception {
        File confFile = temp.newFile("props.cfg");
        FileUtils.writeLines(confFile, Arrays.asList(
                "input.file.name=RESOLVED",
                "-1=ERR",
                "*=ERR2",
                "1-3=ERR3",
                ">1=ERR4",
                "1,2,3=1\\,2\\,3",
                "escape=-escape $${input.file.name}",
                "resolve=-resolve ${input.file.name}[0]"
        ));
        PropertiesConfiguration conf = new PropertiesConfiguration(confFile);
        assertEquals("ERR", conf.getString("-1"));
        assertEquals("ERR2", conf.getString("*"));
        assertEquals("ERR3", conf.getString("1-3"));
        assertEquals("ERR4", conf.getString(">1"));
        assertEquals("1,2,3", conf.getString("1,2,3"));
        assertEquals("-escape ${input.file.name}", conf.getString("escape"));
        assertEquals("-resolve RESOLVED[0]", conf.getString("resolve"));
    }

    @Test
    public void testGetResultParameters() throws Exception {
        File confFile = temp.newFile("props.cfg");
        FileUtils.writeLines(confFile, Arrays.asList(
                "processor.test.param.mime=image/jpeg",
                "processor.test.onExits=0, >10, 2\\,3, *",
                "processor.test.onExit.0.param.file=ERR_0\\:$${input.file.name}",
                "processor.test.onExit.>10.param.file=ERR_>10\\:$${input.file.name}",
                "processor.test.onExit.>10.stop=false",
                "processor.test.onExit.2,3.param.file=ERR_2\\,3\\:$${input.file.name}",
                "processor.test.onExit.*.param.file=ERR_*\\:$${input.file.name}",
                "processor.test.onSkip.param.file=SKIPPED\\:$${input.file.name}"
        ));
        PropertiesConfiguration pconf = new PropertiesConfiguration(confFile);
        String processorId = "processor.test";
        Configuration conf = pconf.subset(processorId);
        conf.setProperty("id", processorId);
        GenericExternalProcess gep = new GenericExternalProcess(conf);
        gep.addInputFile(confFile);
        ProcessResult result = ProcessResult.getResultParameters(conf, gep.getParameters().getMap(), false, 0, null);
        assertEquals("image/jpeg", result.getParameters().get(processorId + ".param.mime"));
        assertEquals("ERR_0:props", result.getParameters().get(processorId + ".param.file"));
        assertEquals(Integer.valueOf(0), result.getExit().getExitCode());
        assertFalse(result.getExit().isStop());
        assertFalse(result.getExit().isSkip());

        result = ProcessResult.getResultParameters(conf, gep.getParameters().getMap(), false, 3, null);
        assertEquals("image/jpeg", result.getParameters().get(processorId + ".param.mime"));
        assertEquals("ERR_2,3:props", result.getParameters().get(processorId + ".param.file"));
        assertEquals(Integer.valueOf(3), result.getExit().getExitCode());
        assertTrue(result.getExit().isStop());
        assertFalse(result.getExit().isSkip());

        result = ProcessResult.getResultParameters(conf, gep.getParameters().getMap(), false, 11, null);
        assertEquals("image/jpeg", result.getParameters().get(processorId + ".param.mime"));
        assertEquals("ERR_>10:props", result.getParameters().get(processorId + ".param.file"));
        assertEquals(Integer.valueOf(11), result.getExit().getExitCode());
        assertFalse(result.getExit().isStop());
        assertFalse(result.getExit().isSkip());

        result = ProcessResult.getResultParameters(conf, gep.getParameters().getMap(), false, -1, null);
        assertEquals("image/jpeg", result.getParameters().get(processorId + ".param.mime"));
        assertEquals("ERR_*:props", result.getParameters().get(processorId + ".param.file"));
        assertEquals(Integer.valueOf(-1), result.getExit().getExitCode());
        assertTrue(result.getExit().isStop());
        assertFalse(result.getExit().isSkip());

        result = ProcessResult.getResultParameters(conf, gep.getParameters().getMap(), true, 0, null);
        assertEquals("image/jpeg", result.getParameters().get(processorId + ".param.mime"));
        assertEquals("SKIPPED:props", result.getParameters().get(processorId + ".param.file"));
        assertFalse(result.getExit().isStop());
        assertTrue(result.getExit().isSkip());
    }

    @Test
    public void testAddInputFile() throws Exception {
        final String inputName = "input.txt";
        final File input = temp.newFile(inputName);
        final BaseConfiguration conf = new BaseConfiguration();
        conf.addProperty(ExternalProcess.PROP_EXEC, "test.sh");
        conf.addProperty(ExternalProcess.PROP_ARG, "-i ${input.file.name}");
        conf.addProperty(ExternalProcess.PROP_ARG, "-path ${input.file}[0]");
        conf.addProperty(ExternalProcess.PROP_ARG, "-o ${input.folder}");
        GenericExternalProcess gp = new GenericExternalProcess(conf).addInputFile(input);
        assertEquals(gp.getParameters().getMap().get(GenericExternalProcess.SRC_NAME_EXT), input.getName());
        assertEquals(gp.getParameters().getMap().get(GenericExternalProcess.SRC_PATH), input.getAbsolutePath());
        assertEquals(gp.getParameters().getMap().get(GenericExternalProcess.SRC_PARENT), input.getParentFile().getAbsolutePath());
        List<String> cmdLines = gp.buildCmdLine(conf);
//        System.out.println(cmdLines.toString());
    }

    @Test
    public void testInterpolateParameters() {
        String testValue = "path=/${PATH1}/${PATH2}/${}";
        ParamHandler paramHandler = new ParamHandler();
        // no params
        assertEquals(testValue, GenericExternalProcess.interpolateParameters(testValue, paramHandler.getMap()));

        paramHandler.add("PATH1", "path1");
        paramHandler.add("PATH2", "$2");
        assertEquals("path=/path1/$2/${}", GenericExternalProcess.interpolateParameters(testValue, paramHandler.getMap()));
    }

}
