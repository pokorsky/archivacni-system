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
package cz.cas.lib.proarc.common.catalog;

import cz.cas.lib.proarc.common.config.CatalogConfiguration;
import cz.cas.lib.proarc.common.config.CatalogQueryField;
import cz.cas.lib.proarc.common.mods.ModsUtils;
import cz.cas.lib.proarc.common.xml.Transformers;
import org.w3c.dom.Element;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Aleph X Server metadata provider
 *
 * @author Jan Pokorsky
 */
public final class AlephXServer implements BibliographicCatalog {

    public static final String TYPE = "AlephXServer";
    public static final String PROPERTY_FIELD_QUERY = "query";
    public static final String PROPERTY_LOAD_BARCODES = "barcode";

    private static final Logger LOG = Logger.getLogger(AlephXServer.class.getName());

    private final Transformers transformers = new Transformers();
    private final URI server;
    private boolean loadBarcodes = false;

    final FieldConfig fields = new FieldConfig();

    public static AlephXServer get(CatalogConfiguration c) {
        if (c == null || !TYPE.equals(c.getType())) {
            return null;
        }

        String url = c.getUrl();
        if (url != null) {
            try {
                AlephXServer aleph = new AlephXServer(url);
                aleph.loadFields(c);

                String loadBarcodes = c.getProperty(PROPERTY_LOAD_BARCODES);
                if (loadBarcodes != null) {
                    aleph.setLoadBarcodes(loadBarcodes.equals("true"));
                }

                return aleph;
            } catch (MalformedURLException | URISyntaxException ex) {
                LOG.log(Level.SEVERE, c.toString(), ex);
            }
        }
        return null;
    }

    private void loadFields(CatalogConfiguration c) {
        List<CatalogQueryField> queryFields = c.getQueryFields();
        for (CatalogQueryField queryField : queryFields) {
            fields.addField(queryField.getName(), queryField.getProperties().getString(PROPERTY_FIELD_QUERY));
        }
    }

    public AlephXServer(URI uri) {
        this.server = uri;
    }

    public AlephXServer(String url) throws URISyntaxException, MalformedURLException {
        // parse with URL that offers better error notification
        this(new URL(url).toURI());
    }

    public List<MetadataItem> find(String fieldName, String value) throws TransformerException, IOException {
        return find(fieldName, value, null);
    }

    @Override
    public List<MetadataItem> find(String fieldName, String value, Locale locale) throws TransformerException, IOException {
        Criteria criteria = fields.getCriteria(fieldName, value);
        if (criteria == null) {
            return Collections.emptyList();
        }
        InputStream is = fetchEntries(criteria);
        FindResponse found = createFindResponse(is);
        if (found == null || found.getEntryCount() < 1) {
            return Collections.emptyList();
        }

        is = fetchDetails(found);
        return createDetailResponse(is, locale);
    }

    public List<MetadataItem> createDetailResponse(InputStream is, Locale locale) throws TransformerException {
        try {
            StreamSource fixedOaiMarc = (StreamSource) transformers.transform(new StreamSource(is), Transformers.Format.AlephOaiMarcFix);
//            StringBuilder sb = new StringBuilder();
//            fixedOaiMarc = (StreamSource) transformers.dump(fixedOaiMarc, sb);
//            String toString = sb.toString();
//            fixedOaiMarc = (StreamSource) transformers.dump2Temp(fixedOaiMarc, "1AlephOaiMarcFix.xml");
            
            DetailResponse details = JAXB.unmarshal(fixedOaiMarc.getInputStream(), DetailResponse.class);
            if (details == null) {
                return Collections.emptyList();
            }
            List<MetadataItem> result = new ArrayList<>();
            for (DetailResponse.Record record : details.getRecords()) {
                Element oaiMarc = record.getOaiMarc();
                DOMSource domSource = new DOMSource(oaiMarc);
                MetadataItem item;
                try {
                    item = createResponse(record.getEntry(), domSource, locale);

                    if (loadBarcodes) {
                        MetadataItem itemWithBarcode;

                        try {
                            itemWithBarcode = addBarcodeMetadata(item, record.getDocNumber());
                        } catch (IOException ex) {
                            LOG.log(Level.SEVERE, null, ex);
                            itemWithBarcode = null;
                        }

                        result.add(itemWithBarcode != null ? itemWithBarcode : item);
                    } else {

                        result.add(item);
                    }

                } catch (UnsupportedEncodingException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            return result;
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setLoadBarcodes(boolean loadBarcodes) {
        this.loadBarcodes = loadBarcodes;
    }

    private MetadataItem addBarcodeMetadata(MetadataItem item, int sysno) throws IOException, TransformerException {
        //add before ending tag of </mods>
        //identifier type="barcode">XXXX</identifier>
        String mods = item.getMods();
        int pos = mods.indexOf("\n</mods>");

        if (pos == -1) {
            LOG.log(Level.WARNING, "Barcode could not be added. Missing ending tag \"</mods>\"");
            return item;
        }

        ItemDataResponse details = null;

        try {
            details = JAXB.unmarshal(fetchItemData(sysno), ItemDataResponse.class);
        } catch (UnknownHostException ex) {
            LOG.log(Level.WARNING, "Unknown host: " + ex.getMessage());
        }

        if (details == null) {
            LOG.log(Level.WARNING, "Could not read item data response. Details null.");
            return item;
        }

        for (ItemDataResponse.Item idr : details.getItems()) {
            String barcode = idr.getBarcode();

            if (barcode == null || barcode.length() != 10 || !barcode.matches("[0-9]+")) {
                LOG.log(Level.WARNING, "Could not load barcode, invalid format: " + barcode);
                continue;
            }

            mods = mods.substring(0,pos) + "\n<identifier type=\"barcode\">" + barcode + "</identifier>" + mods.substring(pos);
        }

        return new MetadataItem(item.getId(), item.getRdczId(), mods, item.getPreview(), item.getTitle());
    }

    FindResponse createFindResponse(InputStream is) {
        try {
            return JAXB.unmarshal(is, AlephXServer.FindResponse.class);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private MetadataItem createResponse(int entryIdx, Source source, Locale locale)
            throws TransformerException, UnsupportedEncodingException {

//        StringBuilder sb = new StringBuilder();
//        source = transformers.dump(source, sb);
//        String toString = sb.toString();
//        source = transformers.dump2Temp(source, "2AlephOaiMarcFixedElement.xml");
        

        Source marcxmlSrc = transformers.transform(source, Transformers.Format.OaimarcAsMarc21slim);
//        marcxmlSrc = transformers.dump2Temp(marcxmlSrc, "3OaimarcAsMarc21slim.xml");
        byte[] modsBytes = transformers.transformAsBytes(
                marcxmlSrc, Transformers.Format.MarcxmlAsMods3);
//        try {
//            FileOutputStream tmp = new FileOutputStream("/tmp/aleph/4mods.xml");
//            tmp.write(modsBytes);
//            tmp.close();
//        } catch (Exception ex) {
//            Logger.getLogger(AlephXServer.class.getName()).log(Level.SEVERE, null, ex);
//        }
        byte[] modsHtmlBytes = modsAsHtmlBytes(new StreamSource(new ByteArrayInputStream(modsBytes)), locale);
        byte[] modsTitleBytes = transformers.transformAsBytes(
                new StreamSource(new ByteArrayInputStream(modsBytes)),
                Transformers.Format.ModsAsTitle);
//        try {
//            FileOutputStream tmp = new FileOutputStream("/tmp/aleph/5title.txt");
//            tmp.write(modsTitleBytes);
//            tmp.close();
//        } catch (Exception ex) {
//            Logger.getLogger(AlephXServer.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return new MetadataItem(entryIdx, new String(modsBytes, "UTF-8"),
                repairHtml(new String(modsHtmlBytes, "UTF-8")), new String(modsTitleBytes, "UTF-8"));
    }

    private String repairHtml(String s) {
        s = s.replaceAll("\\n", "");
        s = s.replaceAll("\\r", "");
        s = replaceMoreSpace(s);
        s = s.replace(") </b>", ") ");
        s = s.replace("( ", "</b>( ");
        s = s.replace("( ", " (");
        s = s.replace("=\" ", " = ");
        s = s.replace("\"", "");
        return s;
    }

    private String replaceMoreSpace(String s) {
        while (s.contains("  ")) {
            s = s.replace("  ", " ");
        }
        return s;
    }

    private byte[] modsAsHtmlBytes(Source source, Locale locale) throws TransformerException {
        byte[] modsHtmlBytes = transformers.transformAsBytes(
                source, Transformers.Format.ModsAsHtml, ModsUtils.modsAsHtmlParameters(locale));
        return modsHtmlBytes;
    }

    private InputStream fetchEntries(Criteria criteria) throws MalformedURLException, IOException {
        URL alephFind = setQuery(server, criteria.toUrlParams(), true).toURL();
        return alephFind.openStream();
    }

    private InputStream fetchDetails(FindResponse found) throws MalformedURLException, IOException {
        String number = found.getNumber();
        int entryCount = found.getEntryCount();
        entryCount = Math.min(10, entryCount);
        String entries = (entryCount == 1) ? "1" : "1-" + entryCount;
        String query = String.format("op=present&set_number=%s&set_entry=%s", number, entries);
        URL alephDetails = setQuery(server, query, false).toURL();
        return new BufferedInputStream(alephDetails.openStream());
    }

    private InputStream fetchItemData(int sysno) throws IOException {
        String query = String.format("op=item-data&doc_num=%s", sysno);
        URL alephDetails = setQuery(server, query, true).toURL();
        return new BufferedInputStream(alephDetails.openStream());
    }

    static URI setQuery(URI u, String newQuery, boolean add) throws MalformedURLException {
        String query = u.getQuery();
        query = (query == null || !add) ? newQuery : query + '&' + newQuery;
        try {
            return  new URI(u.getScheme(), u.getUserInfo(), u.getHost(),
                    u.getPort(), u.getPath(), query, u.getFragment());
        } catch (URISyntaxException ex) {
            MalformedURLException mex = new MalformedURLException(ex.getMessage());
            mex.initCause(ex);
            throw mex;
        }
    }

    static final class FieldConfig {

        private final Map<String, String> values = new HashMap<>();

        public Criteria getCriteria(String fieldName, String value) {
            if (value == null  || value.trim().length() == 0) {
                return null;
            }

            Criteria.Field f = findField(fieldName);
            return f == null ? null : new Criteria(value, f);
        }

        void addField(String key, String alephKeyword) {
            if (key == null || alephKeyword == null) {
                return;
            }

            values.put(key, alephKeyword);
        }

        private Criteria.Field findField(String keyword) {
            String alephKeyword = values.get(keyword);
            return alephKeyword == null ? null : new Criteria.Field(keyword, alephKeyword);
        }
    }

    static final class Criteria {

        private static final class Field {

            private final String alephKeyword;
            private final String keyword;

            private Field(String keyword, String alephKeyword) {
                this.keyword = keyword;
                this.alephKeyword = alephKeyword;
            }

            public String getAlephKeyword() {
                return alephKeyword;
            }

            public String getKeyword() {
                return keyword;
            }

        }

        private String value;
        private Criteria.Field field;

        public Criteria(String value, Criteria.Field field) {
            this.value = value;
            this.field = field;
        }

        public String toUrlParams() {
            String url = String.format("op=find&request=%s=%s",
                    field.getAlephKeyword(), value);
            return url;
        }

    }


    @XmlRootElement(name = "present")
    public static class DetailResponse {

        @XmlElement
        private List<Record> record;

        public DetailResponse() {
        }

        public List<Record> getRecords() {
            return record;
        }


        public static class Record {

            @XmlElement(name = "record_header")
            private Header header;

            @XmlElement(name= "doc_number")
            private int docNumber;

            @XmlElement
            private Metadata metadata;

            public Record() {
            }

            public Header getHeader() {
                return header;
            }

            public Metadata getMetadata() {
                return metadata;
            }

            public int getEntry() {
                return header.getEntry();
            }

            public Element getOaiMarc() {
                return metadata.getOaiMarc();
            }

            public int getDocNumber() {
                return docNumber;
            }

            public static class Header {

                @XmlElement(name = "set_entry")
                private int entry;

                public Header() {
                }

                public int getEntry() {
                    return entry;
                }
            }
        }

        public static class Metadata {

//            @XmlElement(name = "oai_marc")
            @XmlAnyElement(lax=false)
            private Element oaiMarc;

            public Metadata() {
            }

            public Element getOaiMarc() {
                return oaiMarc;
            }
        }
    }

    @XmlRootElement(name = "find")
    public static class FindResponse {

        @XmlElement(name = "set_number")
        private String number;
        @XmlElement(name = "no_records")
        private int recordCount;
        @XmlElement(name = "no_entries")
        private int entryCount;

        public FindResponse() {
        }

        public int getEntryCount() {
            return entryCount;
        }

        public String getNumber() {
            return number;
        }

        public int getRecordCount() {
            return recordCount;
        }
    }

    @XmlRootElement(name = "item-data")
    public static class ItemDataResponse {

        @XmlElement
        private List<Item> item;

        public ItemDataResponse() {
        }

        public List<Item> getItems() {
            if (item == null) {
                return new ArrayList<>();
            }

            return item;
        }

        public static class Item {

            @XmlElement(name = "barcode")
            private String barcode;

            public String getBarcode() {
                return barcode;
            }
        }
    }
}
