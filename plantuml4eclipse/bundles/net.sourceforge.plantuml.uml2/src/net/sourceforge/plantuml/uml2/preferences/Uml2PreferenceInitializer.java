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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import net.sourceforge.plantuml.uml2.PlantUmlOptions;
import net.sourceforge.plantuml.uml2.Uml2Plugin;

public class Uml2PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(Uml2Plugin.PLUGIN_ID);

		prefs.put(Uml2PreferenceConstants.PREF_COMMENT_STYLE, PlantUmlOptions.CommentStyle.TOP_NOTE.name());
		prefs.put(Uml2PreferenceConstants.PREF_NAMING_STYLE, PlantUmlOptions.NamingStyle.QUALIFIED.name());
		prefs.put(Uml2PreferenceConstants.PREF_ELEMENT_COLORS, "");
	}
}
