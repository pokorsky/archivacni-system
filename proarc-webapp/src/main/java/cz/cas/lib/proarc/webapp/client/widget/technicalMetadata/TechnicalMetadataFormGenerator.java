/*
 * Copyright (C) 2020 Lukas Sykora
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
package cz.cas.lib.proarc.webapp.client.widget.technicalMetadata;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import cz.cas.lib.proarc.webapp.client.widget.form.FormGenerator;
import cz.cas.lib.proarc.webapp.client.widget.mods.RepeatableFormItem;
import cz.cas.lib.proarc.webapp.shared.form.Field;
import cz.cas.lib.proarc.webapp.shared.form.FieldBuilder;
import cz.cas.lib.proarc.webapp.shared.form.Form;
import java.util.Map;
import java.util.logging.Logger;

public class TechnicalMetadataFormGenerator extends FormGenerator {

    private static final Logger LOG = Logger.getLogger(TechnicalMetadataFormGenerator.class.getName());

    public static final String HIDDEN_FIELDS_NAME = "__hiddenFields";

    public TechnicalMetadataFormGenerator(Form form, String activeLocale) {
        super(form, activeLocale);
    }

    @Override
    public SelectItem getSelectItem(Field f, String lang) {
        SelectItem item = super.getSelectItem(f, lang);
        item.setAllowEmptyValue(true);
        return item;
    }

    @Override
    public RepeatableFormItem createNestedFormItem(final Field f, final String lang) {
        if (f.getMember(HIDDEN_FIELDS_NAME) == null) {
            f.getFields().add(new FieldBuilder(HIDDEN_FIELDS_NAME)
                    .setTitle("Hodnoty mimo NDK - " + f.getName())
                    .setMaxOccurrences(1).setType(Field.TEXT)
                    .setReadOnly(true)
                    .setHidden(true)
                    .createField());
        }
        RepeatableFormItem rfi = new RepeatableFormItem(f, new RepeatableFormItem.FormWidgetFactory() {

            @Override
            public DynamicForm create() {
                throw new UnsupportedOperationException();
            }

            @Override
            public RepeatableFormItem.FormWidget createFormWidget(Field formField) {
                DynamicForm df = createNestedForm(f, lang);
                ValuesManager vm = new ValuesManager();
                vm.addMember(df);
                return customizeNestedForm(new RepeatableFormItem.FormWidget(df, vm), f);
            }
        });
        rfi.setPrompt(f.getHint(lang));
        oneRow(rfi);
        return rfi;
    }

    @Override
    protected RepeatableFormItem.FormWidget customizeNestedForm(final RepeatableFormItem.FormWidget fw, final Field f) {
        fw.addFormWidgetListener(new RepeatableFormItem.FormWidgetListener() {

            @Override
            public void onDataLoad() {
                ValuesManager vm = fw.getValues();
                Map<?, ?> values = vm.getValues();
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<?, ?> entry : values.entrySet()) {
                    if (HIDDEN_FIELDS_NAME.equals(entry.getKey())) {
                        continue;
                    }
                    Field member = f.getMember(String.valueOf(entry.getKey()));
                    if (member == null) {
                        sb.append(entry.getKey()).append('=').append(entry.getValue());
                        sb.append(", ");
                    }
                }
//                vm.setValue("_hiddenFields", sb.toString());
                DynamicForm memberForField = (DynamicForm) vm.getMemberForField(HIDDEN_FIELDS_NAME);
                FormItem field = memberForField.getField(HIDDEN_FIELDS_NAME);
                field.setVisible(sb.toString().trim().length() != 0);
                field.setValue(sb.toString());
            }
        });
        return fw;
    }
}
