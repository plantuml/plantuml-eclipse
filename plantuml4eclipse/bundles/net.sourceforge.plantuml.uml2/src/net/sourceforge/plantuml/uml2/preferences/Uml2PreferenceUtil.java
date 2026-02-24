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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import net.sourceforge.plantuml.eclipse.Activator;
import net.sourceforge.plantuml.uml2.PlantUmlOptions.CommentStyle;

/**
 * Utility class that returns the preference values
 */
public class Uml2PreferenceUtil {
	protected static ScopedPreferenceStore prefs = null;

	public static void initPrefs() {
		if (prefs == null) {
			prefs = new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
			new Uml2PreferenceInitializer().initializeDefaultPreferences();
		}
	}

	public static CommentStyle getCommentStyle() {
		initPrefs();
		String commentStyle = prefs.getString(Uml2PreferenceConstants.P_COMMENT_STYLE);
		return CommentStyle.valueOf(commentStyle);
	}

	public static boolean useQName() {
		initPrefs();
		return prefs.getBoolean(Uml2PreferenceConstants.P_USE_QNAME);
	}

	public static boolean useAName() {
		initPrefs();
		return prefs.getBoolean(Uml2PreferenceConstants.P_USE_ANAME);
	}
}
