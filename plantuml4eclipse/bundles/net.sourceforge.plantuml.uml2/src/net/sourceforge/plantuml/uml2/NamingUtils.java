/**
 * Copyright (c) 2025 CEA LIST and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 1.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 */

package net.sourceforge.plantuml.uml2;

import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.TypedElement;

import net.sourceforge.plantuml.uml2.PlantUmlOptions.NamingStyle;
import net.sourceforge.plantuml.uml2.preferences.Uml2Preferences;

/**
 * Names with spaces (and other characters to be identified) need to be escaped
 * via "as" directive) The escaped name is referenced by links
 */
public class NamingUtils {
	/**
	 * Name to return, if null
	 */
	final public static String UNDEF = "undefined";

	final protected Package root;

	/**
	 * Create an instance
	 * 
	 * @param root the root package to use for qualified names relative to this package
	 */
	public NamingUtils(Package root) {
		this.root = root;
	}

	/**
	 * @param name
	 * @return the name to use in declarations, containing eventually the as name
	 */
	public static String declName(String name) {
		if (name == null) {
			return UNDEF;
		} else if (needAs(name)) {
			return String.format("\"%s\" as %s", name, asName(name));
		} else {
			return name;
		}
	}

	/**
	 * @param name
	 * @return the name to use in references, i.e. either escaped name or "normal"
	 *         name
	 */
	public static String refName(String name) {
		if (name == null) {
			return UNDEF;
		}
		if (needAs(name)) {
			return asName(name);
		} else {
			return name;
		}
	}

	/**
	 * Return the name, taking into account multiplicity
	 * 
	 * @param ne a named element (which may also be a multiplicity element)
	 * @return the name with a multiplicity indicator, if applicable
	 */
	public String typeName(TypedElement te) {
		return typeName(te, null);
	}

	public String typeName(TypedElement te, Package currentPkg) {

		String qName = refName(getName(te.getType(), currentPkg));
		String multiplicity = "";
		if (te instanceof MultiplicityElement) {
			MultiplicityElement me = (MultiplicityElement) te;
			if (me.getUpper() == -1) {
				multiplicity = " [*]";
			} else if (me.getLower() != 0) {
				if (me.getLower() == 1 && me.getUpper() == 1) {
					// default case of both being 1, don't emit a tag
				} else {
					multiplicity = String.format(" [%d]", me.getUpper());
				}
			} else if (me.getLower() == 0 && me.getUpper() == 1) {
				multiplicity = String.format(" [0..%d]", me.getUpper());
			}
		}
		return String.format("%s%s", qName, multiplicity);
	}

	private static boolean needAs(String name) {
		return name.contains(" ");
	}

	// TODO: somewhat redundant with getLogicalName in AbstractDiagramIntent
	private static String asName(String name) {
		return name.replace(" ", "_");
	}

	/**
	 * @param ne a type
	 * @return the name of a type. If the type is in the current package, just
	 *         return the simple name. Otherwise use the qualified name relative to the root package or the fully
	 *         qualified name
	 */
	public String getName(NamedElement ne, Package currentPkg) {
		if (ne == null) {
			return UNDEF;
		} else if (Uml2Preferences.getNamingStyle() == NamingStyle.SIMPLE || ne.getNearestPackage() == currentPkg) {
			return ne.getName();
		} else if (Uml2Preferences.getNamingStyle() == NamingStyle.QUALIFIED) {
			String qName = getQName(ne);
			return refName(qName).replace(Namespace.SEPARATOR, ".");
		} else { // fully qualified
			String qName = ne.getQualifiedName();
			return refName(qName).replace(Namespace.SEPARATOR, ".");
		}
	}

	/**
	 * @param ne a named element
	 * @return the qualified name relative to the registered root package
	 */
	protected String getQName(NamedElement ne) {
		String qn = ne.getName();
		for (Namespace ns : ne.allNamespaces()) {
			if (ns == root) {
				return qn;
			}
			qn = ns.getName() + "." + qn;
		}
		// root not found, return fully
		return ne.getQualifiedName().replace(Namespace.SEPARATOR, ".");
	}
}
