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
package cz.cas.lib.proarc.common.catalog;

import cz.cas.lib.proarc.common.config.CatalogConfiguration;
import cz.cas.lib.proarc.common.mods.ModsUtils;
import cz.cas.lib.proarc.common.xml.Transformers;
import cz.registrdigitalizace.soapclient.DigitizationRegistryClient;
import cz.registrdigitalizace.soapservices.DigitizationRecord;
import cz.registrdigitalizace.soapservices.DigitizationRegistry;
import cz.registrdigitalizace.soapservices.DigitizationRegistryException_Exception;
import cz.registrdigitalizace.soapservices.PlainQuery;
import cz.registrdigitalizace.soapservices.RecordFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author Jan Pokorsky
 */
public final class DigitizationRegistryCatalog implements BibliographicCatalog {

    public static final String TYPE = "DigitizationRegistryCatalog";
    private static final int MAX_RESULTS = 10;

    private final DigitizationRegistry register;
    private Transformers transformers;

    public static DigitizationRegistryCatalog get(CatalogConfiguration c, String customTemplatePath) {
        if (c == null || !TYPE.equals(c.getType())) {
            return null;
        }
        String url = c.getUrl();
        String user = c.getProperty(CatalogConfiguration.PROPERTY_USER);
        String passwd = c.getProperty(CatalogConfiguration.PROPERTY_PASSWD);
        return new DigitizationRegistryCatalog(url, user, passwd, customTemplatePath);
    }

    public DigitizationRegistryCatalog(String url, String user, String passwd, String customTemplatePath) {
        DigitizationRegistryClient client = new DigitizationRegistryClient(url, user, passwd);
        this.transformers = new Transformers(customTemplatePath);
        register = client.getRegistery();
    }

    @Override
    public List<MetadataItem> find(String catalog, String fieldName, String value, Locale locale) throws TransformerException, IOException {
        PlainQuery query = buildQuery(fieldName, value);
        if (query != null) {
            try {
                List<DigitizationRecord> records = register.findRecords(query, RecordFormat.MARC_XML, MAX_RESULTS);
                return buildResponse(records, locale);
            } catch (DigitizationRegistryException_Exception ex) {
                Logger.getLogger(DigitizationRegistryCatalog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return Collections.emptyList();

    }

    private static PlainQuery buildQuery(String fieldName, String value) {
        PlainQuery query = new PlainQuery();
        if ("issn".equals(fieldName)) {
            query.setIssn(value);
        } else if ("isbn".equals(fieldName)) {
            query.setIsbn(value);
        } else if ("barcode".equals(fieldName)) {
            query.setBarcode(value);
        } else if ("ccnb".equals(fieldName)) {
            query.setCcnb(value);
        } else if ("signature".equals(fieldName)) {
            query.setSignature(value);
        } else if ("title".equals(fieldName)) {
            query.setTitle(value);
        }
        return query;
    }

    private List<MetadataItem> buildResponse(List<DigitizationRecord> records, Locale locale) throws TransformerException, UnsupportedEncodingException {
        ArrayList<MetadataItem> result = new ArrayList<>(records.size());
        int index = 1;
        for (DigitizationRecord record : records) {
            Source descriptor = record.getDescriptor();
            if (descriptor != null) {
                MetadataItem item = createResponse(index++, record.getRecordId(), descriptor, locale);
                result.add(item);
            }
        }
        return result;
    }

    private MetadataItem createResponse(int entryIdx, long recordId, Source marcxmlSrc, Locale locale)
            throws TransformerException, UnsupportedEncodingException {

        byte[] modsBytes = transformers.transformAsBytes(
                marcxmlSrc, Transformers.Format.MarcxmlAsMods3);
        byte[] modsHtmlBytes = modsAsHtmlBytes(new StreamSource(new ByteArrayInputStream(modsBytes)), locale);
        byte[] modsTitleBytes = transformers.transformAsBytes(
                new StreamSource(new ByteArrayInputStream(modsBytes)),
                Transformers.Format.ModsAsTitle);
        if (modsTitleBytes != null && modsTitleBytes.length == 0) {
            modsTitleBytes = transformers.transformAsBytes(new StreamSource(new ByteArrayInputStream(modsBytes)), Transformers.Format.ModsAsAuthorityTitle);
        }
        return new MetadataItem(entryIdx, recordId, new String(modsBytes, "UTF-8"),
                new String(modsHtmlBytes, "UTF-8"), new String(modsTitleBytes, "UTF-8"));
    }

    private byte[] modsAsHtmlBytes(Source source, Locale locale) throws TransformerException {
        byte[] modsHtmlBytes = transformers.transformAsBytes(
                source, Transformers.Format.ModsAsHtml, ModsUtils.modsAsHtmlParameters(locale));
        return modsHtmlBytes;
    }

}
