/*
 * Copyright (C) 2014 Jan Pokorsky
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
package cz.cas.lib.proarc.common.process.export.desa;

import cz.cas.lib.proarc.common.process.export.desa.DesaServices;
import cz.cas.lib.proarc.common.process.export.desa.DesaServices.DesaConfiguration;
import cz.cas.lib.proarc.common.object.ValueMap;
import cz.cas.lib.proarc.common.object.model.MetaModel;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.RecCls;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.RecCls.RecCl;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jan Pokorsky
 */
public class DesaServicesTest {

    private BaseConfiguration conf;
    private DesaServices desaServices;

    public DesaServicesTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        conf = new BaseConfiguration();
        conf.setProperty(DesaServices.PROPERTY_DESASERVICES, "ds1, dsNulls");

        String prefix = DesaServices.PREFIX_DESA + '.' + "ds1" + '.';
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_USER, "ds1user");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_PASSWD, "ds1passwd");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_PRODUCER, "ds1producer");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_OPERATOR, "ds1operator");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_EXPORTMODELS, "model:id1, model:id2");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_RESTAPI, "https://SERVER/dea-frontend/rest/sipsubmission");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_WEBSERVICE, "https://SERVER/dea-frontend/ws/SIPSubmissionService");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_NOMENCLATUREACRONYMS, "acr1, acr2");

        prefix = DesaServices.PREFIX_DESA + '.' + "dsNulls" + '.';
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_USER, null);
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_PASSWD, "");
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_EXPORTMODELS, null);
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_NOMENCLATUREACRONYMS, null);

        prefix = DesaServices.PREFIX_DESA + '.' + "dsNotActive" + '.';
        conf.setProperty(prefix + DesaConfiguration.PROPERTY_USER, "NA");
        desaServices = new DesaServices(conf);
    }

    @Test
    public void testFindConfiguration_MetaModel() {
        MetaModel model = new MetaModel("model:id1", true, true, null, "", "", null, null);
        DesaConfiguration result = desaServices.findConfiguration(model);
        assertNotNull(result);
        assertEquals("ds1", result.getServiceId());
    }

    @Test
    public void testFindConfiguration_MetaModel_Unknown() {
        MetaModel model = new MetaModel("model:unknown", true, true, null, "", "", null, null);
        DesaConfiguration result = desaServices.findConfiguration(model);
        assertNull(result);
    }

    @Test
    public void testFindConfiguration_String() {
        DesaConfiguration ds1 = desaServices.findConfiguration("ds1");
        assertNotNull(ds1);
        assertEquals("ds1", ds1.getServiceId());
        assertEquals(Arrays.asList("model:id1", "model:id2"), ds1.getExportModels());
        assertEquals(Arrays.asList("acr1", "acr2"), ds1.getNomenclatureAcronyms());
        assertEquals("ds1user", ds1.getUsername());
        assertEquals("ds1passwd", ds1.getPassword());
        assertEquals("https://SERVER/dea-frontend/rest/sipsubmission", ds1.getRestServiceUrl());
        assertEquals("https://SERVER/dea-frontend/ws/SIPSubmissionService", ds1.getSoapServiceUrl());
        assertEquals("ds1producer", ds1.getProducer());
        assertEquals("ds1operator", ds1.getOperator());
    }

    @Test
    public void testFindConfiguration_String2() {
        DesaConfiguration ds = desaServices.findConfiguration("dsNulls");
        assertNotNull(ds);
        assertEquals("dsNulls", ds.getServiceId());
        assertEquals(Arrays.asList(), ds.getExportModels());
        assertEquals(Arrays.asList(), ds.getNomenclatureAcronyms());
        assertEquals(null, ds.getUsername());
        assertEquals("", ds.getPassword());
    }

    @Test
    public void testFindConfiguration_String3() {
        DesaConfiguration ds = desaServices.findConfiguration("dsNotActive");
        assertNull(ds);
    }

    @Test
    public void testFindConfigurationWithModel() {
        DesaConfiguration result = desaServices.findConfigurationWithModel("unknown", "model:id1");
        assertNotNull(result);
        assertEquals("ds1", result.getServiceId());

        result = desaServices.findConfigurationWithModel("unknown");
        assertNull(result);
    }

    @Test
    public void testGetValueMap() {
        Nomenclatures n = new Nomenclatures();
        n.setRecCls(new RecCls());
        List<RecCl> recCls = n.getRecCls().getRecCl();
        recCls.add(new RecCl());
        List<ValueMap> result = desaServices.getValueMap(n, "test");
        assertNotNull(result);
        assertEquals("test.rec-cl", result.get(0).getMapId());
    }

}