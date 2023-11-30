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
package cz.cas.lib.proarc.common.xml;

import cz.cas.lib.proarc.common.process.export.mets.ValidationErrorHandler;
import cz.cas.lib.proarc.common.mods.ModsUtils;
import cz.cas.lib.proarc.common.mods.custom.ModsConstants;
import cz.cas.lib.proarc.common.mods.custom.PageMapperTest;
import cz.cas.lib.proarc.mods.ModsDefinition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Jan Pokorsky
 */
public class TransformersTest {

    private static final Logger LOG = Logger.getLogger(TransformersTest.class.getName());
    private static ProxySelector defaultProxy;
    private static List<URI> externalConnections;

    public TransformersTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        defaultProxy = ProxySelector.getDefault();
        // detect external connections
        ProxySelector.setDefault(new ProxySelector() {

            @Override
            public List<Proxy> select(URI uri) {
                externalConnections.add(uri);
                return defaultProxy.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                defaultProxy.connectFailed(uri, sa, ioe);
            }
        });
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        externalConnections = new ArrayList<URI>();
        XMLUnit.setIgnoreWhitespace(true);
    }

    @After
    public void tearDown() {
        assertTrue(externalConnections.toString(), externalConnections.isEmpty());
        XMLUnit.setIgnoreWhitespace(false);
        XMLUnit.setNormalizeWhitespace(false);
    }

    @Test
    public void testMarcAsMods() throws Exception {
        XMLUnit.setNormalizeWhitespace(true);
        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMods.xml");
        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMarcXml.xml");// from test
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
            close(goldenIS);
        }
    }

    /**
     * Tests mapping of 910a(sigla) as {@code <physicalLocation>} and 910b(signatura) as {@code <shelfLocator>}.
     */
    @Test
    public void testMarcAsMods_Issue32() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            XMLAssert.assertXpathEvaluatesTo("HKA001", "/m:mods/m:location/m:physicalLocation[1]", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("test sigla", "/m:mods/m:location/m:physicalLocation[2]", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("2", "count(/m:mods/m:location/m:physicalLocation)", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("54 487", "/m:mods/m:location/m:shelfLocator[1]", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("test signatura", "/m:mods/m:location/m:shelfLocator[2]", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("2", "count(/m:mods/m:location/m:shelfLocator)", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of fields 310a and 008/18 to {@code frequency@authority}.
     * See issue 118 and 181.
     */
    @Test
    public void testMarcAsMods_FrequencyAuthority_Issue181() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("frequencyAuthority.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            XMLAssert.assertXpathNotExists("/m:mods/m:originInfo/m:frequency[1]/@authority", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("2x ročně", "/m:mods/m:originInfo/m:frequency[1]", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("marcfrequency", "/m:mods/m:originInfo/m:frequency[2]/@authority", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("Semiannual", "/m:mods/m:originInfo/m:frequency[2]", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of fields 600, 610, 611, 630, 648, 650, 651 indicator_9 $2 to {@code subject@authority}.
     * See issue 182.
     */
    @Test
    public void testMarcAsMods_SubjectAuthority_Issue182() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_subject_65X_X9.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            // 600
            XMLAssert.assertXpathExists("/m:mods/m:subject[@authority='czenas']/m:name[@type='personal']/m:namePart[text()='Novák, A. Jiří']", xmlResult);
            // 650
            XMLAssert.assertXpathExists("/m:mods/m:subject[@authority='czenas']/m:topic[text()='daňové delikty']", xmlResult);
            XMLAssert.assertXpathExists("/m:mods/m:subject[@authority='eczenas']/m:topic[text()='tax delinquency']", xmlResult);
            // 651
            XMLAssert.assertXpathExists("/m:mods/m:subject[@authority='czenas']/m:geographic[text()='Česko']", xmlResult);
            XMLAssert.assertXpathExists("/m:mods/m:subject[@authority='eczenas']/m:geographic[text()='Czechia']", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of field 653 indicator_9 $a to {@code subject/topic@lang}.
     * See issue 185.
     * See issue 433.
     */
    @Test
    public void testMarcAsMods_SubjectTopic_Issue185_Issue433() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_subject_65X_X9.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            // 653
            XMLAssert.assertXpathExists("/m:mods/m:subject[not(@authority)]/m:topic[text()='kočky' and @lang='cze']", xmlResult);
            XMLAssert.assertXpathExists("/m:mods/m:subject[not(@authority)]/m:topic[text()='cats' and @lang='eng']", xmlResult);
            XMLAssert.assertXpathNotExists("/m:mods/m:subject/m:name/m:namePart[text()='kočky']", xmlResult);
            XMLAssert.assertXpathNotExists("/m:mods/m:subject/m:name/m:namePart[text()='cats']", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Test mapping of 072#7 $x to {@code subject/topic} and $a/$9 to {@code classification}.
     * See issue 303.
     */
    @Test
    public void testMarcAsMods_Conspectus_Issue303() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            XMLAssert.assertXpathEvaluatesTo("Umění", "/m:mods/m:subject[@authority='Konspekt']/m:topic", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("7.01/.09", "/m:mods/m:classification[@authority='udc' and @edition='Konspekt']", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("21", "/m:mods/m:classification[@authority='Konspekt']", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of field 100,700 $7 to {@code name@authorityURI} and {@code name@valueURI}.
     * See issue 305.
     */
    @Test
    public void testMarcAsMods_AuthorityId_Issue305() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_subject_65X_X9.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            // test 100 1# $a Kocina, Jan, $d 1960- $4 aut $7 xx0113245
            XMLAssert.assertXpathExists("/m:mods/m:name[@type='personal'"
                        + " and @authorityURI='http://aut.nkp.cz'"
                        + " and @valueURI='http://aut.nkp.cz/xx0113245']"
                    + "/m:namePart[@type='family' and text()='Kocina']"
                    + "/../m:namePart[@type='given' and text()='Jan']"
                    + "/../m:namePart[@type='date' and text()='1960-']"
                    + "/../m:role/m:roleTerm[text()='aut']", xmlResult);
            // test 700 1# $a Honzík, Bohumil, $d 1972- $4 aut $7 jn20020422016
            XMLAssert.assertXpathExists("/m:mods/m:name[@type='personal'"
                        + " and @authorityURI='http://aut.nkp.cz'"
                        + " and @valueURI='http://aut.nkp.cz/jn20020422016']"
                    + "/m:namePart[@type='family' and text()='Honzík']"
                    + "/../m:namePart[@type='given' and text()='Bohumil']"
                    + "/../m:namePart[@type='date' and text()='1972-']"
                    + "/../m:role/m:roleTerm[text()='aut']", xmlResult);
            // test 700 1# $a Test Without AuthorityId $d 1972- $4 aut
            XMLAssert.assertXpathExists("/m:mods/m:name[@type='personal'"
                        + " and not(@authorityURI)"
                        + " and not(@valueURI)]"
                    + "/m:namePart[text()='Test Without AuthorityId']"
                    + "/../m:namePart[@type='date' and text()='1972-']"
                    + "/../m:role/m:roleTerm[text()='aut']", xmlResult);

            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of field 510 $c to {@code part/detail/number}.
     * See issue 306.
     */
    @Test
    public void testMarcAsMods_RelatedItemPartDetailNumber_Issue306() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_subject_65X_X9.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            // test 510 4# $a Knihopis 1 $c K01416
            XMLAssert.assertXpathExists("/m:mods/m:relatedItem[@type='isReferencedBy']"
                    + "/m:titleInfo/m:title[text()='Knihopis 1']", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("K01416", "/m:mods/m:relatedItem[@type='isReferencedBy']"
                    + "/m:titleInfo/m:title[text()='Knihopis 1']/../../m:part/m:detail[@type='part']/m:number", xmlResult);

            // test 510 0# $a Knihopis 2
            XMLAssert.assertXpathExists("/m:mods/m:relatedItem[@type='isReferencedBy']"
                    + "/m:titleInfo/m:title[text()='Knihopis 2']", xmlResult);
            XMLAssert.assertXpathNotExists("/m:mods/m:relatedItem[@type='isReferencedBy']"
                    + "/m:titleInfo/m:title[text()='Knihopis 2']/../../m:part", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of field 787 to {@code relatedItem}.
     * See issue 313.
     */
    @Test
    public void testMarcAsMods_RelatedItem_Issue313() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_relatedItem_787.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            // test 78708 |i Recenze na: |a Čeřovský, Jan |t Jak jsme zachraňovali svět aneb Půl století ve službách ochrany přírody |d Praha : Nakladatelství Academia, 2014 |4 kniha
            XMLAssert.assertXpathEvaluatesTo(
                    "Jak jsme zachraňovali svět aneb Půl století ve službách ochrany přírody",
                    "/m:mods/m:relatedItem[not(@type) and @displayLabel='Recenze na:']"
                    + "/m:titleInfo/m:title/text()", xmlResult);
            XMLAssert.assertXpathEvaluatesTo(
                    "Čeřovský, Jan",
                    "/m:mods/m:relatedItem[not(@type) and @displayLabel='Recenze na:']"
                    + "/m:name/m:namePart/text()", xmlResult);
            XMLAssert.assertXpathEvaluatesTo(
                    "Praha : Nakladatelství Academia, 2014",
                    "/m:mods/m:relatedItem[not(@type) and @displayLabel='Recenze na:']"
                    + "/m:originInfo/m:publisher/text()", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of field 520 and subfield $9 to {@code abstract@lang}.
     * See issue 434.
     */
    @Test
    public void testMarcAsMods_AbstractLang_Issue434() throws Exception {
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_subject_65X_X9.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            XMLAssert.assertXpathExists("/m:mods/m:abstract[@lang='cze' and @type='Abstract' and text()='Text cze']", xmlResult);
            XMLAssert.assertXpathExists("/m:mods/m:abstract[@lang='eng' and @type='Abstract' and text()='Text eng']", xmlResult);
            XMLAssert.assertXpathExists("/m:mods/m:abstract[not(@lang) and @type='Abstract' and text()='Text no lang']", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    /**
     * Tests mapping of field 264_4 to {@code originInfo}.
     * See issue 298.
     */
    @Test
    public void testMarcAsMods_Mapping264_ind4_Issue298() throws Exception{
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("marc_originInfo_264_ind4.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods3);
            assertNotNull(contents);
            String xmlResult = new String(contents, "UTF-8");
//            System.out.println(xmlResult);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(new HashMap() {{
                put("m", ModsConstants.NS);
            }}));
            XMLAssert.assertXpathExists("/m:mods/m:originInfo[@eventType='copyright']", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("Praha","/m:mods/m:originInfo[@eventType='copyright']/m:place/m:placeTerm", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("Albatros","/m:mods/m:originInfo[@eventType='copyright']/m:publisher", xmlResult);
            XMLAssert.assertXpathEvaluatesTo("2015", "/m:mods/m:originInfo[@eventType='copyright']/m:copyrightDate", xmlResult);
            validateMods(new StreamSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
        }
    }

    @Test
    public void testOaiMarcAsMarc() throws Exception {
        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMarcXml.xml");
        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsOaiMarc.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.OaimarcAsMarc21slim);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
            close(goldenIS);
        }
    }

    @Test
    public void testAlephXServerDetailNamespaceFix() throws Exception {
        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseFixed.xml");
        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("../catalog/alephXServerDetailResponse.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.AlephOaiMarcFix);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
            close(goldenIS);
        }
    }

    @Test
    public void testModsAsHtml() throws Exception {
//        XMLUnit.setNormalizeWhitespace(true);
//        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMods.xml");
//        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMods.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);
        Map<String, Object> params = ModsUtils.modsAsHtmlParameters(Locale.ENGLISH);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.ModsAsHtml, params);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
//            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
//            close(goldenIS);
        }
    }

    @Test
    public void testModsAsFedoraLabel_Page() throws Exception {
        assertEquals("[1], Blank",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("page_mods.xml"), "model:page"));
    }

    @Test
    public void testModsAsFedoraLabel_Issue() throws Exception {
        assertEquals("1",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("issue_mods.xml"), "model:periodicalitem"));
    }

    @Test
    public void testModsAsFedoraLabel_Volume() throws Exception {
        assertEquals("1893, 1",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("volume_mods.xml"), "model:periodicalvolume"));
    }

    /** Tests label with date but missing volume number. */
    @Test
    public void testModsAsFedoraLabel_Volume_issue222() throws Exception {
        assertEquals("1893",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("volume_mods_issue222.xml"), "model:periodicalvolume"));
    }

    @Test
    public void testModsAsFedoraLabel_Periodical() throws Exception {
        assertEquals("MTITLE[0]: STITLE[0]",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("periodical_mods.xml"), "model:periodical"));
    }

    @Test
    public void testModsAsFedoraLabel_Empty() throws Exception {
        String label = ModsUtils.getLabel(new ModsDefinition(), "model:page");
        assertEquals("?", label);
    }

    private String modsAsFedoraLabel(InputStream xmlIS, String model) throws Exception {
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers(null);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("MODEL", model);
        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.ModsAsFedoraLabel, params);
            assertNotNull(contents);
            String label = new String(contents, "UTF-8");
//            System.out.println(label);
            return label;
        } finally {
            close(xmlIS);
        }
    }

    private void validateMods(Source source) throws Exception {
        Validator v = ModsUtils.getSchema().newValidator();
        ValidationErrorHandler handler = new ValidationErrorHandler();
        v.setErrorHandler(handler);
        v.validate(source);
        List<String> errors = handler.getValidationErrors();
        assertTrue(errors.toString(), errors.isEmpty());
    }

    private static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

}
