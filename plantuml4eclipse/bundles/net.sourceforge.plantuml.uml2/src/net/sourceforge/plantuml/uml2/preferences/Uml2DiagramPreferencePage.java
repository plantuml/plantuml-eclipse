/**
 * Copyright (c) 2026 CEA LIST and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 */
package net.sourceforge.plantuml.uml2.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.sourceforge.plantuml.uml2.PlantUmlOptions.CommentStyle;
import net.sourceforge.plantuml.uml2.PlantUmlOptions.NamingStyle;
import net.sourceforge.plantuml.uml2.Uml2Plugin;

public class Uml2DiagramPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(final IWorkbench workbench) {
		IPreferenceStore store = Uml2Plugin.getPreferences();
		setPreferenceStore(store);
		setDescription("This preferences page allows to customize PlantUML output from UML2 models"); //$NON-NLS-1$
	}

	protected ComboFieldEditor commentStyleFE;

	protected ComboFieldEditor namingStyleFE;

	protected StringFieldEditor colorFE;

	@Override
	protected void createFieldEditors() {
		List<String[]> commentOptions = new ArrayList<String[]>();
		for (CommentStyle lit : CommentStyle.values()) {
			String name = lit.name();
			commentOptions.add(new String[] { name, name });
		}
		List<String[]> namingOptions = new ArrayList<String[]>();
		for (NamingStyle lit : NamingStyle.values()) {
			String name = lit.name();
			namingOptions.add(new String[] { name, name });
		}

		commentStyleFE = new ComboFieldEditor(Uml2PreferenceConstants.PREF_COMMENT_STYLE, "Comment options", //$NON-NLS-1$
				commentOptions.toArray(new String[2][0]), getFieldEditorParent());
		addField(commentStyleFE);

		namingStyleFE = new ComboFieldEditor(Uml2PreferenceConstants.PREF_NAMING_STYLE, "Naming options", //$NON-NLS-1$
				namingOptions.toArray(new String[2][0]), getFieldEditorParent());
		addField(namingStyleFE);

		colorFE = new StringFieldEditor(Uml2PreferenceConstants.PREF_ELEMENT_COLORS,
				"Coloring options (comma separated pairs, e.g. DataType:#302030, Class:#604020)", //$NON-NLS-1$
				getFieldEditorParent());
		addField(colorFE);
	}
}
