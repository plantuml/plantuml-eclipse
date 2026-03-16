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

import net.sourceforge.plantuml.uml2.PlantUmlOptions.CommentStyle;
import net.sourceforge.plantuml.uml2.PlantUmlOptions.NamingStyle;
import net.sourceforge.plantuml.uml2.Uml2Plugin;

/**
 * Utility class that returns the preference values
 */
public class Uml2Preferences {

	public static CommentStyle getCommentStyle() {
		String commentStyle = Uml2Plugin.getPreferences().getString(Uml2PreferenceConstants.PREF_COMMENT_STYLE);
		return CommentStyle.valueOf(commentStyle);
	}

	public static NamingStyle getNamingStyle() {
		String namingStyle = Uml2Plugin.getPreferences().getString(Uml2PreferenceConstants.PREF_NAMING_STYLE);
		return NamingStyle.valueOf(namingStyle);
	}

	/**
	 * Return the element color
	 * 
	 * @param elementName (e.g. Class, DataType)
	 * @return the found color string or null
	 */
	public static String getElementColor(String elementName) {
		String elementColors = Uml2Plugin.getPreferences().getString(Uml2PreferenceConstants.PREF_ELEMENT_COLORS);
		int index = elementColors.indexOf(elementName + ":");
		if (index >= 0) {
			String remainder = elementColors.substring(index + elementName.length() + 1);
			if (remainder.length() >= 7) {
				return remainder.substring(0, 7);
			}
		}
		return null;
	}
}
