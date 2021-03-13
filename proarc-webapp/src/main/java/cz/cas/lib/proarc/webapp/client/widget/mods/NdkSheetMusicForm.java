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
package cz.cas.lib.proarc.webapp.client.widget.mods;

import cz.cas.lib.proarc.common.mods.custom.ModsConstants;
import cz.cas.lib.proarc.webapp.shared.form.Field;
import cz.cas.lib.proarc.webapp.shared.form.FieldBuilder;
import cz.cas.lib.proarc.webapp.shared.form.Form;
import java.util.List;

/**
 * The NDK Sheet Music.
 *
 * Version 1.1_2
 *
 * @author Jan Pokorsky
 */
public final class NdkSheetMusicForm {

    public Form build() {
        Form f = new Form();

        f.getFields().add(NdkForms.descriptionRadioButton());

        Field mods = new FieldBuilder("mods").setMaxOccurrences(1).createField();
        f.getFields().add(mods);
        List<Field> modsFields = mods.getFields();

        modsFields.add(titleInfo(true));
        modsFields.add(name(true));
        modsFields.add(typeOfResource());
        modsFields.add(genre(true));
        modsFields.add(originInfo(true));
        modsFields.add(language(true));
        modsFields.add(physicalDescription(true));
        modsFields.add(abstracts());
        modsFields.add(note());
        modsFields.add(subject());
        modsFields.add(classification());
        modsFields.add(identifier(true));
        modsFields.add(part());
        modsFields.add(NdkForms.recordInfo());
        modsFields.add(relatedItem());
        return f;
    }



        protected Field titleInfo(boolean required) {
            // titleInfo, titleInfoDefinition
            return new FieldBuilder("titleInfo").setTitle("Title Info - M").setMaxOccurrences(10)
                    .setHint("Název titulu.<p>Pro plnění použít katalogizační záznam.")
                    // titleInfo@type, enum
                    .addField(new FieldBuilder("type").setTitle("Type - MA").setMaxOccurrences(1).setType(Field.SELECT)
                            .setHint("Pokud jde o hlavní název, pak nechat tuto hodnotu prázdnou.<dl>Jinak použít jednu z hodnot:"
                                    + "<dt>abbreviated</dt><dd>zkrácený název</dd>"
                                    + "<dt>alternative</dt><dd>alternativní název</dd>"
                                    + "<dt>translated</dt><dd>přeložený název</dd>"
                                    + "<dt>uniform</dt><dd>stejný/jednotný název</dd>"
                                    + "</dl>")
                            .addMapValue("abbreviated", "Abbreviated")
                            .addMapValue("alternative", "Alternative")
                            .addMapValue("translated", "Translated")
                            .addMapValue("uniform", "Uniform")
                            .createField())
                    // title, type="stringPlusLanguage"
                    .addField(new FieldBuilder("title").setMaxOccurrences(1)
                            .addField(new FieldBuilder("value").setTitle("Title - M").setMaxOccurrences(1).setType(Field.TEXT).setRequired(required)
                                    .setHint("Název hudebniny.")
                                    .createField()) // value
                            // lang, String
                            // xmlLang, lang
                            // script, String
                            // transliteration, String
                            .createField()) // title
                    // subTitle, type="stringPlusLanguage"
                    .addField(new FieldBuilder("subTitle").setMaxOccurrences(1)
                            .addField(new FieldBuilder("value").setTitle("Subtitle - MA").setMaxOccurrences(1).setType(Field.TEXT)
                                    .setHint("Podnázev hudebniny.")
                                    .createField()) // value
                            // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                            .createField()) // subTitle
                    // partNumber, type="stringPlusLanguage"
                    .addField(new FieldBuilder("partNumber").setMaxOccurrences(1)
                            // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                            .addField(new FieldBuilder("value").setTitle("Part Number - MA").setMaxOccurrences(1).setType(Field.TEXT)
                                    .setHint("Číslo části.")
                                    .createField()) // value
                            .createField()) // partNumber
                    // partName, type="stringPlusLanguage"
                    .addField(new FieldBuilder("partName").setMaxOccurrences(1)
                            // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                            .addField(new FieldBuilder("value").setTitle("Part Name - MA").setMaxOccurrences(1).setType(Field.TEXT)
                                    .setHint("Název části.")
                                    .createField()) // value
                            .createField()) // partName
                    // nonSort, type="stringPlusLanguage"
                    // titleInfo@attributes: otherType, supplied, altRepGroup, altFormatAttributeGroup, nameTitleGroup, usage, ID, authorityAttributeGroup, xlink:simpleLink, languageAttributeGroup, displayLabel
                    .createField();
        }

        protected Field name(boolean required) {
            // name, nameDefinition
            return new FieldBuilder("name").setMaxOccurrences(10).setTitle("Name - MA")
                    .setHint("Údaje o odpovědnosti."
                            + "<p>Pokud má hudebnina jiné původce než je autor,"
                            + " element <name> se opakuje s různými rolemi"
                            + " (skladatel, autor textu apod.).")
                    // @ID, @authorityAttributeGroup, @xlinkSimpleLink, @languageAttributeGroup, @displayLabel, @altRepGroup, @nameTitleGroup
                    // @type(personal, corporate, conference, family)
                    .addField(new FieldBuilder("type").setTitle("Type - MA").setMaxOccurrences(1).setType(Field.SELECT).setRequired(required)
                            .setHint("<dl>"
                                    + "<dt>personal</dt><dd>celé jméno osoby</dd>"
                                    + "<dt>corporate</dt><dd>název společnosti, instituce nebo organizace</dd>"
                                    + "<dt>conference</dt><dd>název konference nebo související typ setkání</dd>"
                                    + "<dt>family</dt><dd>rodina/rod</dd>"
                                    + "</dl>")
                            .addMapValue("personal", "personal")
                            .addMapValue("corporate", "corporate")
                            .addMapValue("conference", "conference")
                            .addMapValue("family", "family")
                            .createField()) // @type
                    // @usage(fixed="primary")
                    .addField(new FieldBuilder("usage").setTitle("Usage - O").setMaxOccurrences(1).setType(Field.SELECT).setDefaultValue("")
                            .setHint("Hodnota “primary” pro označení primární autority.")
                            .addMapValue("", "")
                            .addMapValue("primary", "primary")
                            .createField()) // usage
                    // namePart, namePartDefinition extends stringPlusLanguage
                    .addField(new FieldBuilder("namePart").setTitle("Name Parts - M").setMaxOccurrences(5)
                            // @type(date, family, given, termsOfAddress)
                            .addField(new FieldBuilder("type").setTitle("Type - MA").setMaxOccurrences(1).setType(Field.SELECT)
                                    .setHint("<dl>"
                                            + "<dt>date</dt><dd>RA - datum</dd>"
                                            + "<dt>family</dt><dd>MA -příjmení </dd>"
                                            + "<dt>given</dt><dd>MA - jméno/křestní jméno</dd>"
                                            + "<dt>termsOfAddress</dt><dd>RA - tituly a jiná slova nebo čísla související se jménem</dd>"
                                            + "</dl>")
                                    .addMapValue("date", "date")
                                    .addMapValue("family", "family")
                                    .addMapValue("given", "given")
                                    .addMapValue("termsOfAddress", "termsOfAddress")
                                    .createField()) // @type
                            // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                            .addField(new FieldBuilder("value").setTitle("Name Part - M").setMaxOccurrences(1)
                                    .setType(Field.TEXT).setRequired(required)
                                    .setHint("Údaje o křestním jméně, příjmení apod."
                                            + "<p>Nutno vyjádřit pro křestní jméno i příjmení."
                                            + "<p>Pokud nelze rozlišit křestní jméno a příjmení,"
                                            + " nepoužije se type a jméno se zaznamená"
                                            + " v podobě jaké je do jednoho elementu &lt;namePart>.")
                                    .createField()) // value
                            .createField()) // namePart
                    // displayForm
                    .addField(new FieldBuilder("etal").setMaxOccurrences(1)
                            .addField(new FieldBuilder("value").setMaxOccurrences(1).setTitle("Etal - O").setType(Field.TEXT)
                                    .setHint("Element indikující, že existuje více autorů, než pouze ti, kteří byli uvedeni v <name> elementu." +
                                            "<p>V případě užití tohoto elementu je dále top element <name> neopakovatelný." +
                                            "<p><etal> je nutné umístit do samostatného top elementu <name>, ve kterém se " +
                                            "nesmí objevit subelementy <namePart> a <nameIdentifier>." +
                                            "<p><etal> je neopakovatelný element, který se do zápisu vkládá ručně.").createField())
                            .createField()) //etal
                    // affiliation
                    // role, roleDefinition
                    .addField(new FieldBuilder("nameIdentifier").setTitle("Name identifier - RA").setMaxOccurrences(5)
                            .addField(new FieldBuilder("value").setMaxOccurrences(1)
                                    .setType(Field.TEXT).setRequired(false).setHint("Číslo národní autority").createField())
                            .createField()) //nameIdentifier
                    .addField(new FieldBuilder("role").setTitle("Role - M").setMaxOccurrences(5)
                            .setHint("Specifikace role osoby nebo organizace uvedené v elementu &lt;name>")
                            // roleTerm, type="roleTermDefinition" extends stringPlusLanguagePlusAuthority
                            .addField(NdkForms.roleTerm(
                                    "Role Term - M", required, "Authority - M", required, "Type - M", required
                            )) // roleTerm
                            .createField()) // role
                    // description
                    .createField(); // name
        }

        protected Field typeOfResource() {
            // typeOfResource, typeOfResourceDefinition extends resourceTypeDefinition
            return new FieldBuilder("typeOfResource").setMaxOccurrences(1)
                    // typeOfResourceDefinition
                    //   collection
                    //   manuscript
                    //   displayLabel
                    //   altRepGroup
                    //   usage
                    // resourceTypeDefinition
                    .addField(new FieldBuilder("value").setTitle("Type of Resource - R").setMaxOccurrences(1).setType(Field.SELECT)
                            .setHint("Popis charakteristiky typu nebo obsahu zdroje.<p>Pro hudebniny hodnota „notated music“.")
                            .addMapValue("notated music", "notated music")
                            .createField())
                    .createField(); // typeOfResource
        }

    protected Field genre(boolean required) {
        // genre, genreDefinition extends stringPlusLanguagePlusAuthority extends stringPlusLanguage
        return new FieldBuilder("genre").setTitle("Genre - M").setMaxOccurrences(10)
                .setHint("Bližší údaje o typu dokumentu.<p>Pro hudebniny hodnota „sheetmusic“.")
                // genreDefinition@attributes: type, displayLabel, altRepGroup, usage
                // stringPlusLanguagePlusAuthority: authorityAttributeGroup: @authority, @authorityURI, @valueURI
                // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXT).setDefaultValue("sheetmusic").setRequired(required).createField())
                .createField(); // genre
    }

    protected Field originInfo(boolean required) {
        // originInfo, originInfoDefinition
        return new FieldBuilder("originInfo").setTitle("Origin Info - M").setMaxOccurrences(10)
                .setHint("Informace o původu předlohy.")
                // @languageAttributeGroup(lang, XmlLang, script, transliteration)
//                .addField(new FieldBuilder("transliteration").setTitle("Transliteration").setMaxOccurrences(1).setType(Field.COMBO)
//                    .addMapValue("printer", "Tiskař")
//                .createField())
                // @displayLabel
                // @altRepGroup
                // eventType
                .addField(new FieldBuilder("eventType").setTitle("Event Type - M").setMaxOccurrences(1).setType(Field.COMBO)
                        .setHint("Hodnoty dle druhého indikátoru pole 264:"
                                + "<p>264_0 production (R) se uvádí, jestliže pole obsahuje údaje o vytvoření zdroje v nezveřejněné podobě."
                                + "<p>264_1 publication (R) se uvádí, jestliže pole obsahuje údaje o nakladateli zdroje."
                                + "<p>264_2 distribution (R) se uvádí, jestliže pole obsahuje údaje o distribuci zdroje."
                                + "<p>264_3 manufacture (R) se uvádí, jestliže pole obsahuje údaje o tisku, výrobě zdroje ve zveřejněné podobě."
                                + "<p>264_4 copyright (R) se uvádí, jestliže pole obsahuje údaje o ochraně podle autorského práva (copyright).")
                        .addMapValue("", "")
                        .addMapValue(ModsConstants.VALUE_ORIGININFO_EVENTTYPE_PRODUCTION, ModsConstants.VALUE_ORIGININFO_EVENTTYPE_PRODUCTION)
                        .addMapValue(ModsConstants.VALUE_ORIGININFO_EVENTTYPE_PUBLICATION, ModsConstants.VALUE_ORIGININFO_EVENTTYPE_PUBLICATION)
                        .addMapValue(ModsConstants.VALUE_ORIGININFO_EVENTTYPE_DISTRIBUTION, ModsConstants.VALUE_ORIGININFO_EVENTTYPE_DISTRIBUTION)
                        .addMapValue(ModsConstants.VALUE_ORIGININFO_EVENTTYPE_MANUFACTURE, ModsConstants.VALUE_ORIGININFO_EVENTTYPE_MANUFACTURE)
                        .addMapValue(ModsConstants.VALUE_ORIGININFO_EVENTTYPE_COPYRIGHT, ModsConstants.VALUE_ORIGININFO_EVENTTYPE_COPYRIGHT)
                        .createField()) // eventType
                // place, placeDefinition
                .addField(new FieldBuilder("place").setMaxOccurrences(1)
                        // @supplied
                        // placeTerm, placeTermDefinition extends stringPlusLanguage
                        .addField(new FieldBuilder("placeTerm").setMaxOccurrences(1)
                                // type, codeOrText('code', 'text')
                                .addField(new FieldBuilder("type").setTitle("Type - M").setMaxOccurrences(1).setType(Field.SELECT).setDefaultValue("text")
                                        .setHint("Typ popisu místa. Kódem nebo textově."
                                                + "<p>Pokud má dokument více míst vydání v poli 260, podpole „a“, přebírají se ze záznamu všechna místa"
                                                + "<li>“code” pro údaj z pole 008</li><li>“text” pro údaj z pole 260</li>")
                                        .addMapValue("code", "code")
                                        .addMapValue("text", "text")
                                        .createField()) // type
                                // @authorityURI, @valueURI,@authority
                                .addField(new FieldBuilder("authority").setTitle("Authority - MA").setMaxOccurrences(1).setType(Field.COMBO)
                                        .setHint("Hodnota “marccountry” jen u údaje z pole 008")
                                        .addMapValue("marccountry", "marccountry")
                                        .createField()) // @authority
                                // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                                .addField(new FieldBuilder("value").setTitle("Place Term - MA").setMaxOccurrences(1).setType(Field.TEXT)
                                        .setHint("Konkrétní určení místa a země vydání, např. Praha resp. xr pro ČR."
                                                + "<p>Odpovídá hodnotám z katalogizačního záznamu, pole 260, podpole „a“ resp. pole 008/15-17.")
                                        .createField()) // value
                                .createField()) // placeTerm
                        .createField()) // place
                // publisher, stringPlusLanguagePlusSupplied
                .addField(new FieldBuilder("publisher").setTitle("Publisher - MA").setMaxOccurrences(10)
                        // stringPlusLanguagePlusSupplied: @supplied
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Jméno entity, která dokument vydala, vytiskla nebo jinak vyprodukovala."
                                        + "<p>Odpovídá poli 260 podpoli „b“ katalogizačního záznamu v MARC21;"
                                        + "<p>Pokud má titul více vydavatelů, přebírají se ze záznamu všichni (jsou v jednom poli 260).")
                                .createField()) // value
                        .createField()) // publisher
                // dateIssued, dateDefinition extends stringPlusLanguage
                .addField(new FieldBuilder("dateIssued").setTitle("Date Issued - M").setMaxOccurrences(10)
                        .setHint("Datum vydání předlohy."
                                + "<p>Odpovídá hodnotě z katalogizačního záznamu, pole 260, podpole „c“ a pole 008/07-10.")
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @encoding(w3cdtf, iso8601, marc, temper, edtf), @qualifier, @point(start, end), @keyDate
                        .addField(new FieldBuilder("encoding").setTitle("Encoding - R").setMaxOccurrences(1).setType(Field.SELECT)
                                .setHint("Kódování - hodnota „marc“ jen u údaje z pole 008.")
                                .addMapValue("iso8601", "ISO 8601")
                                .addMapValue("edtf", "EDTF")
                                .addMapValue("marc", "MARC")
                                .addMapValue("temper", "temper")
                                .addMapValue("w3cdtf", "W3CDTF")
                                .createField()) // @encoding
                        .addField(new FieldBuilder("point").setTitle("Point - MA").setMaxOccurrences(1).setType(Field.SELECT)
                                .setHint("Hodnoty „start“ resp. „end“ jen u údaje z pole 008, pro rozmezí dat.")
                                .addMapValue("start", "start")
                                .addMapValue("end", "end")
                                .createField()) // @point
                        .addField(new FieldBuilder("qualifier").setTitle("Qualifier - R").setMaxOccurrences(1).setType(Field.SELECT)
                                .setHint("Možnost dalšího upřesnění, hodnota „approximate“ pro data, kde nevíme přesný údaj. Hodnota  „inferred“ pro odvozený nebo dopočítaný údaj")
                                .addMapValue("approximate", "Approximate")
                                .addMapValue("inferred", "Inferred")
                                .createField()) // @qualifier
                        .addField(new FieldBuilder("value").setTitle("Date - M").setMaxOccurrences(1).setType(Field.TEXT).setWidth("200")
                                .setHint("Datum vydání předlohy."
                                        + "<p>Odpovídá hodnotě z katalogizačního záznamu, pole 260, podpole „c“ a pole 008/07-10.")
                                .createField()) // value
                        .createField()) // dateIssued
                // dateOther, dateOtherDefinition extends dateDefinition
                .addField(new FieldBuilder("dateOther").setMaxOccurrences(1)
                        .addField(new FieldBuilder("value").setTitle("Date Other - R").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Datum vytvoření, distribuce, výroby předlohy."
                                        + "<p>Tento elemet se využije v případě výskytu $c v:"
                                        + "<p>264_0 je production"
                                        + "<p>264_2 je distribution"
                                        + "<p>264_3 je manufacture")
                                .createField()) // value
                        .createField()) // dateOther
                // copyrightDate, dateDefinition extends stringPlusLanguage
                .addField(new FieldBuilder("copyrightDate").setMaxOccurrences(1)
                        .addField(new FieldBuilder("value").setTitle("Copyright Date - R").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Využije se pouze v případě výskuytu pole 264 s druhým indikátorem \"4\" a podpolem $c.")
                                .createField()) // value
                        .createField()) // copyrightDate
                // dateCreated, dateDefinition extends stringPlusLanguage
                // dateCaptured
                // dateValid
                // dateModified
                // edition, type="stringPlusLanguagePlusSupplied"
                // issuance, issuanceDefinition, enum
                .addField(new FieldBuilder("issuance").setTitle("Issuance - M").setMaxOccurrences(1).setType(Field.SELECT).setRequired(required)
                        .setHint("Údaje o vydávání.<p>Odpovídá hodnotě uvedené v návěští MARC21 na pozici 07.")
                        .addMapValue("continuing", "continuing")
                        .addMapValue("monographic", "monographic")
                        .addMapValue("single unit", "single unit")
                        .addMapValue("multipart monograph", "multipart monograph")
                        .addMapValue("serial", "serial")
                        .addMapValue("integrating resource", "integrating resource")
                        .createField()) // issuance
                // frequency, stringPlusLanguagePlusAuthority
                .createField(); // originInfo
    }

    protected Field language(boolean required) {

        // language, languageDefinition
        return new FieldBuilder("language").setTitle("Languages - M").setMaxOccurrences(10)
                .setHint("Údaje o jazyce dokumentu; v případě vícenásobného výskytu nutno element &lt;language> opakovat")
                // @objectPart, @displayLabel, @altRepGroup, @usage
                .addField(new FieldBuilder("objectPart").setTitle("Object Part - MA").setMaxOccurrences(1).setType(Field.COMBO).setWidth("300")
                        .setHint("Možnost vyjádřit jazyk konkrétní části svazku.")
                        .addMapValue("summary", "summary")
                        .addMapValue("table of contents", "table of contents")
                        .addMapValue("accompanying material", "accompanying material")
                        .addMapValue("translation", "translation")
                        .createField()) // @objectPart
                // languageAttributeGroup: @lang, @xmlLang, @script, @transliteration
                // languageTerm, languageTermDefinition
                .addField(new FieldBuilder("languageTerm").setMaxOccurrences(1)
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @authorityURI, @valueURI
                        // @authority, enum
                        .addField(new FieldBuilder("authority").setTitle("Authority - M").setMaxOccurrences(1)
                                .setType(Field.SELECT).setRequired(required)
                                .setHint("Použít hodnotu „iso639-2b“.")
                                .addMapValue("iso639-2b", "ISO 639-2B")
                                .addMapValue("rfc3066", "RFC 3066")
                                .addMapValue("iso639-3", "ISO 639-3")
                                .addMapValue("rfc4646", "RFC 4646")
                                .addMapValue("rfc5646", "RFC 5646")
                                .createField()) // authority
                        // type, codeOrText('code', 'text')
                        .addField(new FieldBuilder("type").setTitle("Type - M").setMaxOccurrences(1)
                                .setType(Field.SELECT).setRequired(required)
                                .setHint("Typ popisu. Použít hodnotu code")
                                .addMapValue("code", "code")
                                .addMapValue("text", "text")
                                .createField()) // type
                        .addField(NdkForms.createLangTermValue(required)
                                .createField()) // value
                        .createField()) // languageTerm
                // scriptTerm
                .createField(); // language
    }

    protected Field physicalDescription(boolean required) {
        // physicalDescription, physicalDescriptionDefinition
        return new FieldBuilder("physicalDescription").setTitle("Physical Description - M").setMaxOccurrences(10)
                .setHint("Obsahuje údaje o fyzickém popisu zdroje/předlohy.")
                // form, formDefinition extends stringPlusLanguagePlusAuthority
                .addField(new FieldBuilder("form").setTitle("Form - M").setMaxOccurrences(1)
                        // stringPlusLanguagePlusAuthority: authorityAttributeGroup: @authority, @authorityURI, @valueURI
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @type
                        // XXX autofill "marcform"
                        .addField(new FieldBuilder("authority").setTitle("Authority - M").setMaxOccurrences(1).setType(Field.COMBO)
                                .addMapValue(ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_MARCFORM, ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_MARCFORM)
                                .addMapValue(ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_MARCCATEGORY, ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_MARCCATEGORY)
                                .addMapValue(ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_MARCSMD, ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_MARCSMD)
                                .addMapValue(ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_GMD, ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_GMD)
                                .addMapValue(ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_RDAMEDIA, ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_RDAMEDIA)
                                .addMapValue(ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_RDACARRIER, ModsConstants.VALUE_PHYSICALDESCRIPTION_FORM_RDACARRIER)
                                .createField()) // authority
                        .addField(new FieldBuilder("value").setTitle("Form - M").setMaxOccurrences(1)
                                .setType(Field.COMBO).setRequired(required).setHint("form")
                                .setHint("Údaje o fyzické podobě dokumentu, např. print, electronic, microfilm apod."
                                        + "<p>Odpovídá hodnotě v poli 008/23")
                                .addMapValue("braille", "braille")
                                .addMapValue("electronic", "electronic")
                                .addMapValue("large print", "large print")
                                .addMapValue("microfilm", "microfilm")
                                .addMapValue("microfiche", "microfiche")
                                .addMapValue("print", "print")
                                .addMapValue("jiný", "jiný")
                                .addMapValue("audio", "rdamedia - audio")
                                .addMapValue("počítač", "rdamedia - počítač")
                                .addMapValue("mikroforma", "rdamedia - mikroforma")
                                .addMapValue("mikroskop", "rdamedia - mikroskop")
                                .addMapValue("projekce", "rdamedia - projekce")
                                .addMapValue("stereograf", "rdamedia - stereograf")
                                .addMapValue("bez media", "rdamedia - bez media")
                                .addMapValue("video", "rdamedia - video")
                                .addMapValue("svazek", "rdacarrier - svazek")
                                .addMapValue("online zdroj", "rdacarrier - online zdroj")
                                .addMapValue("audiodisk", "rdacarrier - audiodisk")
                                .addMapValue("počítačový disk", "rdacarrier - počítačový disk")
                                .createField()) // value
                        .createField()) // form
                // reformattingQuality
                // internetMediaType
                // digitalOrigin
                // extent, stringPlusLanguagePlusSupplied
                .addField(new FieldBuilder("extent").setTitle("Extent - RA").setMaxOccurrences(5)
                        // stringPlusLanguagePlusSupplied: @supplied
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        .addField(new FieldBuilder("value").setTitle("Extent - RA").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Údaje o rozsahu (stran, svazků nebo rozměrů)"
                                        + "<p>Odpovídá hodnotě v poli 300, podpole „a“, „b“ a „c“"
                                        + "<p>Počet stránek bude vyjádřen ve fyzické strukturální mapě")
                                .createField()) // value
                        .createField()) // extent
                // note, physicalDescriptionNote extends stringPlusLanguage
                .addField(new FieldBuilder("note").setTitle("Note - RA").setMaxOccurrences(5)
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @displayLabel, @type, @typeURI, @xlinkSimpleLink, @ID
                        .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXTAREA)
                                .setHint("Poznámka o fyzickém stavu dokumentu."
                                        + "<p>Pro každou poznámku je nutno vytvořit nový &lt;note> element.")
                                .createField()) // value
                        .createField()) // note
                .createField(); // physicalDescription
    }

    protected Field abstracts() {
        // abstract, abstractDefinition extends stringPlusLanguage
        return new FieldBuilder("abstract").setTitle("Abstract - R").setMaxOccurrences(10)
                // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                // @displayLabel, @type, @xlink:simpleLink, @shareable, @altRepGroup
                // altFormatAttributeGroup: @altFormat, @contentType
                .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXTAREA).setLength(2700)
                        .setHint("Shrnutí obsahu jako celku. Odpovídá poli 520 MARC21")
                        .createField()) // value
                .createField(); // abstract
    }

    protected Field note() {
        // note, noteDefinition extends stringPlusLanguage
        return new FieldBuilder("note").setTitle("Note - RA").setMaxOccurrences(30)
                // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                // @displayLabel, @type, @typeURI, @xlink:simpleLink, @ID, @altRepGroup
                .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXTAREA)
                        .setHint("Obecná poznámka k titulu jako celku."
                                + "<p>Odpovídá hodnotám v poli 245, podpole „c“ (statement of responsibility)"
                                + " a v polích 5XX (poznámky) katalogizačního záznamu")
                        .createField()) // value
                .createField(); // note
    }

    protected Field subject() {
        // subject, subjectDefinition
        return new FieldBuilder("subject").setTitle("Subject - R").setMaxOccurrences(30)
                .setHint("Údaje o věcném třídění.")
                // @ID, @authorityAttributeGroup, @languageAttributeGroup, @xlink:simpleLink, @displayLabel, @altRepGroup, @usage
                // autofill "czenas"
                .addField(new FieldBuilder("authority").setTitle("Authority - R").setMaxOccurrences(1).setType(Field.COMBO)
                        .addMapValue("czenas", "czenas")
                        .addMapValue("eczenas", "eczenas")
                        .addMapValue("Konspekt", "Konspekt")
                        .createField())

                // topic, stringPlusLanguagePlusAuthority
                .addField(new FieldBuilder("topic").setMaxOccurrences(1)
                        // stringPlusLanguagePlusAuthority: authorityAttributeGroup: @authority, @authorityURI, @valueURI
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
//                    // XXX autofill "marcform"
//                    .addField(new FieldBuilder("authority").setTitle("Authority").setMaxOccurrences(1).setType(Field.TEXT).setWidth("200").createField())
                        // @type
                        // XXX autority.nkp.cz datasource
                        .addField(new FieldBuilder("value").setTitle("Topic - R").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Libovolný výraz specifikující nebo charakterizující obsah dokumentu."
                                        + "<p>Použít kontrolovaný slovník - např. z báze autorit AUT NK ČR (věcné téma)"
                                        + " nebo obsah pole 650 záznamu MARC21 nebo obsah pole 072 $x.")
                                .createField()) // value
                        .createField()) // topic

                // geographic, stringPlusLanguagePlusAuthority

                // temporal, temporalDefinition extends dateDefinition extends stringPlusLanguage
                .addField(new FieldBuilder("temporal").setMaxOccurrences(1)
                        // authorityAttributeGroup: @authority, @authorityURI, @valueURI
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @encoding, @qualifier, @point, @keyDate
                        // XXX autority.nkp.cz datasource
                        .addField(new FieldBuilder("value").setTitle("Temporal - R").setMaxOccurrences(1).setType(Field.TEXT).setWidth("200")
                                .setHint("Chronologické věcné třídění."
                                        + "<p>Použít kontrolovaný slovník - např. z báze autorit AUT NK ČR (chronologický údaj)"
                                        + " nebo obsah pole 648 záznamu MARC21.")
                                .createField()) // value
                        .createField()) // temporal

                // titleInfo, subjectTitleInfoDefinition
                // name, subjectNameDefinition
                .addField(nameInSubject()
                        /*new FieldBuilder("name").setMaxOccurrences(1)
                    // @type, enum: personal, corporate, ...
                    // @ID, @xlink:simpleLink, displayLabel
                    // languageAttributeGroup: @lang, @xmlLang, @script, @transliteration
                    // authorityAttributeGroup: @authority, @authorityURI, @valueURI
                    // namePart, namePartDefinition extends stringPlusLanguage
                    .addField(new FieldBuilder("namePart").setMaxOccurrences(1)
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @type, enum: date, family, given, termsOfAddress
                        .addField(new FieldBuilder("value").setTitle("Name Part - M").setMaxOccurrences(1).setType(Field.TEXT)
                            .setHint("Jméno použité jako věcné záhlaví."
                                + "<p>Použít kontrolovaný slovník ‐ např. z báze autorit AUT NK ČR (jméno osobní)"
                                + " nebo obsah pole 600 záznamu MARC21."
                                + "<p>Celé jméno se zapíše do tohoto elementu.")
                        .createField()) // value
                    .createField()) // namePart
                    // displayForm
                    // affiliation
                    // role
                    // description
                .createField()*/
                ) // name

                // geographicCode
                // hierarchicalGeographic
                // cartographics
                // occupation
                // genre
                .createField(); // subject
    }

    protected Field classification() {
        // classification, classificationDefinition extends stringPlusLanguagePlusAuthority
        return new FieldBuilder("classification").setTitle("Classification - R").setMaxOccurrences(10)
                // stringPlusLanguagePlusAuthority: authorityAttributeGroup: @authority, @authorityURI, @valueURI
                // autofill "udc"
                .addField(new FieldBuilder("authority").setTitle("Authority - M").setMaxOccurrences(1).setType(Field.COMBO)
                        .addMapValue("udc", "udc")
                        .addMapValue("Konspekt", "Konspekt")
                        .createField()) // authority
                .addField(new FieldBuilder("edition").setTitle("Edition - M").setMaxOccurrences(1).setType(Field.COMBO)
                        .addMapValue("", "")
                        .addMapValue("Konspekt", "Konspekt")
                        .createField()) // edition
                .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXT)
                        .setHint("Klasifikační údaje věcného třídění podle Mezinárodního"
                                + " desetinného třídění. Odpovídá poli 080 MARC21."
                                + "<p>Klasifikační údaje věcného třídění podle Konspektu."
                                + " Odpovídá poli 072 $a MARC21.")
                        .createField()) // value
                .createField(); // classification
    }

        // XXX unsupported yet
        // relatedItem

    protected Field identifier(boolean required) {
        // identifier, identifierDefinition, [0,*]
        return new FieldBuilder("identifier").setTitle("Identifier - M").setMaxOccurrences(10)
                .setHint("Údaje o identifikátorech.<p>Obsahuje unikátní identifikátory"
                        + " mezinárodní nebo lokální."
                        + "<p>Uvádějí se i neplatné resp. zrušené identifikátory - atribut invalid=“yes“.")
                // stringPlusLanguage@languageAttributeGroup
                //   lang, xs:string
                //   xml:lang
                //   script, xs:string
                //   transliteration, xs:string
                //   type, xs:string
                .addField(new FieldBuilder("type").setTitle("Type - M").setMaxOccurrences(1).setType(Field.COMBO).setRequired(required)
                        .setHint("UUID - M - vygeneruje dodavatel"
                                + "<br>čČNB - MA - převzít z katalogizačního záznamu z pole 015, podpole „a“, „z“"
                                + "<br>ISBN - MA - převzít z katalogizačního záznamu z pole 020, podpole „a“, „z“"
                                + "<br>ISMN - MA - převzít z katalogizačního záznamu z pole 024, podpole „a“, „z“"
                                + "<br>URN:NBN - M - zápis ve tvaru urn:nbn:cz:ndk-123456 pro projekt NDK"
                                + "<br>jiný interní identifikátor - R - type = barcode, oclc, sysno, permalink apod.")
                        // XXX use ValueMap
                        .addMapValue("barcode", "Čárový kód")
                        .addMapValue("ccnb", "čČNB")
                        .addMapValue("doi", "DOI")
                        .addMapValue("hdl", "Handle")
                        .addMapValue("isbn", "ISBN")
                        .addMapValue("ismn", "ISMN")
//                    .addMapValue("issn", "ISSN")
                        .addMapValue("permalink", "Permalink")
                        .addMapValue("sici", "SICI")
                        .addMapValue("url", "URL")
                        .addMapValue("urnnbn", "URN:NBN")
                        .addMapValue("uuid", "UUID")
                        .addMapValue("oclc", "OCLC")
                        .createField())
                // stringPlusLanguage/value
                .addField(new FieldBuilder("value").setTitle("Identifier - M").setMaxOccurrences(1).setType(Field.TEXT).setRequired(required).createField())
                // identifierDefinition
                //   displayLabel, xs:string
                //   typeURI, xs:anyURI
                //   invalid, fixed="yes"
                .addField(new FieldBuilder("invalid").setTitle("Invalid - MA").setMaxOccurrences(1).setType(Field.SELECT).setDefaultValue("")
                        .addMapValue("", "Platný")
                        .addMapValue("yes", "Neplatný")
                        .createField()) // invalid
                //   altRepGroup, xs:string
                .createField(); // identifier
    }

    protected Field location(boolean required) {
        // location, locationDefinition
        return new FieldBuilder("location").setTitle("Location - MA").setMaxOccurrences(10)
                .setHint("Údaje o uložení popisovaného dokumentu, např. signatura, místo uložení apod.")
                // languageAttributeGroup: @lang, @xmlLang, @script, @transliteration
                // @displayLabel, @altRepGroup
                // physicalLocation, physicalLocationDefinition extends stringPlusLanguagePlusAuthority
                .addField(new FieldBuilder("physicalLocation").setTitle("Physical Location - M").setMaxOccurrences(1)
                        // stringPlusLanguagePlusAuthority: authorityAttributeGroup: @authority, @authorityURI, @valueURI
                        // autofill "siglaADR"
                        .addField(new FieldBuilder("authority").setTitle("Authority - O").setMaxOccurrences(1).setType(Field.TEXT).setDefaultValue("siglaADR").createField())
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        // @xlink:simpleLink, @displayLabel, @type
                        .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXT).setRequired(required)
                                .setHint("Údaje o instituci, kde je fyzicky uložen popisovaný dokument. Např. NK ČR."
                                        + "<p>Nutno použít kontrolovaný slovník - sigly knihovnen (ABA001 atd.)"
                                        + "<p>Odpovídá poli 910 $a v MARC21."
                                        + "<p>Pozn. u dokumentů v digitální podobě není možné vyplnit.")
                                .createField()) // value
                        .createField()) // physicalLocation
                // shelfLocator, stringPlusLanguage
                .addField(new FieldBuilder("shelfLocator").setTitle("Shelf Locator - M").setMaxOccurrences(10)
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        .addField(new FieldBuilder("value").setMaxOccurrences(1).setType(Field.TEXT).setRequired(required)
                                .setHint("Signatura nebo lokační údaje o daném konkrétním dokumentu, který slouží jako předloha.")
                                .createField()) // value
                        .createField()) // shelfLocator
                // url, urlDefinition extends xs:anyURI
                .addField(new FieldBuilder("url").setTitle("URL - O").setMaxOccurrences(10)
                        // @dateLastAccessed, @displayLabel, @access(preview, raw object, object in context), @usage(primary display, primary)
                        // @note
                        .addField(new FieldBuilder("note").setTitle("Note - O").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Pro poznámku o typu URL (na plný text, abstrakt apod.)")
                                .createField()) // note
                        .addField(new FieldBuilder("value").setTitle("URL - O").setMaxOccurrences(1).setType(Field.TEXT)
                                .setHint("Pro uvedení lokace elektronického dokumentu.")
                                .createField()) // value
                        .createField()) // url
                // holdingSimple
                // holdingExternal
                .createField(); // location
    }

    protected Field part() {
        // part, type="partDefinition"
        return new FieldBuilder("part").setTitle("Part - O").setMaxOccurrences(1)
                .setHint("Popis části, pokud je svazek částí souboru.")
                // @ID, @type, @order, @displayLabel, @altRepGroup
                // @languageAttributeGroup(lang, XmlLang, script, transliteration)
                .addField(new FieldBuilder("type").setTitle("Type - O").setMaxOccurrences(1).setType(Field.TEXT).setDefaultValue("volume")
                        .setHint("Hodnota bude vždy „volume“.")
                        .createField()) // type
                // detail, type="detailDefinition"
                .addField(new FieldBuilder("detail").setMaxOccurrences(1)
                        // @type, level
                        // number
                        // caption, type="stringPlusLanguage"
                        .addField(new FieldBuilder("caption").setMaxOccurrences(1)
                                // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                                .addField(new FieldBuilder("value").setTitle("Detail Caption - RA").setMaxOccurrences(1).setType(Field.COMBO)
                                        .setHint("Text před označením čísla.")
                                        .addMapValue("č.", "č.")
                                        .addMapValue("část", "část")
                                        .addMapValue("No.", "No.")
                                        .createField()) // value
                                .createField()) // caption
                        // title
                        .createField()) // detail
                // extent, type="extentDefinition"
                // date
                // text
                .createField(); // part
    }

    private Field nameInSubject() {
        // name, nameDefinition
        return new FieldBuilder("name").setMaxOccurrences(10).setTitle("Name - R")
                .setHint("Údaje o odpovědnosti za svazek."
                        + "<p>Pokud má monografie autora a ilustrátora, element &lt;name> se opakuje s různými rolemi.")
                // @ID, @authorityAttributeGroup, @xlinkSimpleLink, @languageAttributeGroup, @displayLabel, @altRepGroup, @nameTitleGroup
                // @type(personal, corporate, conference, family)
                .addField(new FieldBuilder("type").setTitle("Type - R").setMaxOccurrences(1).setType(Field.SELECT)
                        // issue: 612 not required
                        .setRequired(false)
                        .setHint("<dl>"
                                + "<dt>personal</dt><dd>celé jméno osoby</dd>"
                                + "<dt>corporate</dt><dd>název společnosti, instituce nebo organizace</dd>"
                                + "<dt>conference</dt><dd>název konference nebo související typ setkání</dd>"
                                + "<dt>family</dt><dd>rodina/rod</dd>"
                                + "</dl>")
                        .addMapValue("personal", "personal")
                        .addMapValue("corporate", "corporate")
                        .addMapValue("conference", "conference")
                        .addMapValue("family", "family")
                        .createField()) // @type
                // @usage(fixed="primary")
                .addField(new FieldBuilder("usage").setTitle("Usage - O").setMaxOccurrences(1).setType(Field.SELECT).setDefaultValue("")
                        .setHint("Hodnota “primary” pro označení primární autority.")
                        .addMapValue("", "")
                        .addMapValue("primary", "primary")
                        .createField()) // usage
                // namePart, namePartDefinition extends stringPlusLanguage
                .addField(new FieldBuilder("namePart").setTitle("Name Parts - R").setMaxOccurrences(5)
                        // @type(date, family, given, termsOfAddress)
                        .addField(new FieldBuilder("type").setTitle("Type - R").setMaxOccurrences(1).setType(Field.SELECT)
                                .setHint("<dl>"
                                        + "<dt>date</dt><dd>RA - datum</dd>"
                                        + "<dt>family</dt><dd>MA -příjmení </dd>"
                                        + "<dt>given</dt><dd>MA - jméno/křestní jméno</dd>"
                                        + "<dt>termsOfAddress</dt><dd>RA - tituly a jiná slova nebo čísla související se jménem</dd>"
                                        + "</dl>")
                                .addMapValue("date", "date")
                                .addMapValue("family", "family")
                                .addMapValue("given", "given")
                                .addMapValue("termsOfAddress", "termsOfAddress")
                                .createField()) // @type
                        // stringPlusLanguage: @lang, @xmlLang, @script, @transliteration
                        .addField(new FieldBuilder("value").setTitle("Name Part - R").setMaxOccurrences(1)
                                .setType(Field.TEXT)
                                // issue: 612 not required
                                .setRequired(false)
                                .setHint("Údaje o křestním jméně, příjmení apod."
                                        + "<p>Nutno vyjádřit pro křestní jméno i příjmení."
                                        + "<p>Pokud nelze rozlišit křestní jméno a příjmení,"
                                        + " nepoužije se type a jméno se zaznamená"
                                        + " v podobě jaké je do jednoho elementu &lt;namePart>"
                                        + "<p>Pokud známe datum narození a úmrtí autora, vyplnit"
                                        + " ve tvaru RRRR-RRRR s atributem type=”date”.")
                                .createField()) // value
                        .createField()) // namePart
                // displayForm
                .addField(new FieldBuilder("etal").setMaxOccurrences(1)
                        .addField(new FieldBuilder("value").setMaxOccurrences(1).setTitle("Etal - O").setType(Field.TEXT)
                                .setHint("Element indikující, že existuje více autorů, než pouze ti, kteří byli uvedeni v <name> elementu." +
                                        "<p>V případě užití tohoto elementu je dále top element <name> neopakovatelný." +
                                        "<p><etal> je nutné umístit do samostatného top elementu <name>, ve kterém se " +
                                        "nesmí objevit subelementy <namePart> a <nameIdentifier>." +
                                        "<p><etal> je neopakovatelný element, který se do zápisu vkládá ručně.").createField())
                        .createField()) //etal
                // affiliation
                // role, roleDefinition
                .addField(new FieldBuilder("nameIdentifier").setTitle("Name Identifier - R").setMaxOccurrences(5)
                        .addField(new FieldBuilder("value").setMaxOccurrences(1)
                                .setType(Field.TEXT).setRequired(false).setHint("Číslo národní autority").createField())
                        .createField()) //nameIdentifier
                .addField(new FieldBuilder("role").setTitle("Role - R").setMaxOccurrences(5)
                        .setHint("Specifikace role osoby nebo organizace uvedené v elementu &lt;name>")
                        // roleTerm, type="roleTermDefinition" extends stringPlusLanguagePlusAuthority
                        // issue: 612 not required
                        .addField(NdkForms.roleTerm(
                                "Role Term - R", false, "Authority - R", false, "Type - M", false
                        )) // roleTerm
                        .createField()) // role
                // description
                .createField(); // name
    }

    private Field relatedItem() {
        return new FieldBuilder("relatedItem").setTitle("Related Item - RA").setMaxOccurrences(10)
                .addField(new FieldBuilder("type").setTitle("Type - R").setMaxOccurrences(1)
                        .setHint("Type spolu s otherType popisují vztah položky, popsané " +
                                "v <relatedItem> a dokumentu, který je předmětem MODS záznamu").setType(Field.SELECT)
                        .addMapValue("preceding", "preceding")
                        .addMapValue("succeeding", "succeeding")
                        .addMapValue("original", "original")
                        .addMapValue("host", "host")
                        .addMapValue("constituent", "constituent")
                        .addMapValue("series", "series")
                        .addMapValue("otherVersion", "otherVersion")
                        .addMapValue("otherFormat", "otherFormat")
                        .addMapValue("isReferencedBy", "isReferencedBy")
                        .addMapValue("references", "references")
                        .addMapValue("reviewOf", "reviewOf")
                        .createField())
                .addField(new FieldBuilder("otherType").setTitle("Other Type - O").setMaxOccurrences(1)
                        .setHint("Type spolu s otherType popisují vztah položky, popsané " +
                                "v <relatedItem> a dokumentu, který je předmětem MODS záznamu").setType(Field.TEXT).createField())
                .addField(new FieldBuilder("otherTypeURI").setTitle("Other Type URI - O").setMaxOccurrences(1)
                        .setHint("Odkaz na zdroj položky v <relatedItem>, který se vztahuje k popisovanému").setType(Field.TEXT).createField())
                .addField(new FieldBuilder("otherTypeAuth").setTitle("Other Type Auth - O").setMaxOccurrences(1)
                        .setHint("Autoritní záznam příbuzné položky").setType(Field.TEXT).createField())
                .addField(new FieldBuilder("otherTypeAuthURI").setTitle("Other Type Auth URI - O").setMaxOccurrences(1)
                        .setHint("Odkaz na autoritní záznam příbuzné položky").setType(Field.TEXT).createField())
                .addField(titleInfo(false))
                .addField(name(false))
                .addField(typeOfResource())
                .addField(genre(false))
                .addField(originInfo(false))
                .addField(language(false))
                .addField(physicalDescription(false))
                .addField(abstracts())
                .addField(note())
                .addField(subject())
                .addField(classification())
                .addField(identifier(false))
                .addField(part())
                .addField(location(false))
                //.addField(NdkForms.recordInfo())
                .createField();
    }

}
