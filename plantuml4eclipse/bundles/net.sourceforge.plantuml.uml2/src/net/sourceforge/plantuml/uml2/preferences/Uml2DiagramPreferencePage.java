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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import net.sourceforge.plantuml.eclipse.Activator;
import net.sourceforge.plantuml.uml2.PlantUmlOptions.CommentStyle;

public class Uml2DiagramPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(final IWorkbench workbench) {
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
		setPreferenceStore(store);
		setDescription("This preferences page allows to customize PlantUML output from UML2 models"); //$NON-NLS-1$
		new Uml2PreferenceInitializer().initializeDefaultPreferences();
	}

	protected ComboFieldEditor commentStyleFE;

	protected BooleanFieldEditor useQNameFE;

	protected BooleanFieldEditor useANameFE;

	@Override
	protected void createFieldEditors() {
		List<String[]> commentOptions = new ArrayList<String[]>();
		for (CommentStyle lit : CommentStyle.values()) {
			String name = lit.name();
			commentOptions.add(new String[] { name, name });
		}

		commentStyleFE = new ComboFieldEditor(Uml2PreferenceConstants.P_COMMENT_STYLE, "Comment options", //$NON-NLS-1$
				commentOptions.toArray(new String[2][0]), getFieldEditorParent());
		addField(commentStyleFE);

		useQNameFE = new BooleanFieldEditor(Uml2PreferenceConstants.P_USE_QNAME, "Use qualified names",
				getFieldEditorParent());
		addField(useQNameFE);

		useANameFE = new BooleanFieldEditor(Uml2PreferenceConstants.P_USE_ANAME, "Use absolute names",
				getFieldEditorParent());
		addField(useANameFE);
	}
}
