/**
 * Copyright (c) 2025, 2026 CEA LIST and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 */

package net.sourceforge.plantuml.uml2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.uml2.common.util.UML2Util;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;

import net.sourceforge.plantuml.uml2.PlantUmlOptions.CommentStyle;

/**
 * Utility functions for comments.
 */
public class CommentUtils {
	/**
	 * Convenience function that appends without indent.
	 * 
	 * @see append
	 * 
	 * @param el a UML element containing eventually comments)
	 * @param sb a string-builder to append to
	 */
	public static void append(Element el, StringBuilder sb) {
		append(el, sb, false);
	}

	/**
	 * Add owned comments of an element (if any) in form of a PlantUML comment ('
	 * prefix).
	 * 
	 * @param el a UML element containing eventually comments)
	 * @param sb a string-builder to append to
	 * @param if true, prefix each line with a tab
	 */
	public static void append(Element el, StringBuilder sb, boolean indent) {
		boolean simpleComment;
		if (el instanceof Classifier || el instanceof Package) {
			simpleComment = PlantUmlOptions.exportComments == CommentStyle.SIMPLE;
		} else {
			// always uses simple comments for non-classifiers and non-packages
			simpleComment = PlantUmlOptions.exportComments != CommentStyle.NONE;
		}
		if (simpleComment) {
			for (Comment comment : getComments(el)) {
				if (comment.getBody().length() > 0) {
					for (String cLine : comment.getBody().split("\n")) {
						if (indent) {
							sb.append("\t");
						}
						sb.append("'" + cLine + "\n");
					}
				}
			}
		}
	}

	/**
	 * Convenience function that appends without indent.
	 * 
	 * @see appendNote
	 * 
	 * @param el a UML element containing eventually comments)
	 * @param sb a string-builder to append to
	 */
	public static void appendNote(NamedElement ne, StringBuilder sb) {
		appendNote(ne, sb, false);
	}

	/**
	 * Add owned comments of an element (if any) in form of a PlantUML note.
	 * 
	 * @param el a UML element containing eventually comments)
	 * @param sb a string-builder to append to
	 * @param if true, prefix each line with a tab
	 */
	public static void appendNote(NamedElement ne, StringBuilder sb, boolean indent) {
		boolean noteComment;
		if (ne instanceof Classifier || ne instanceof Package) {
			noteComment = PlantUmlOptions.exportComments == CommentStyle.TOP_NOTE
					|| PlantUmlOptions.exportComments == CommentStyle.BOTTOM_NOTE;
		} else {
			// never use note comments for non-classifiers and non-packages
			noteComment = false;
		}
		if (noteComment) {
			String position = null;
			if (PlantUmlOptions.exportComments == CommentStyle.TOP_NOTE) {
				position = "top";
			} else {
				position = "bottom";
			}
			for (Comment comment : getComments(ne)) {
				if (comment.getBody().length() > 0) {
					if (indent) {
						sb.append("\t");
					}
					String body = comment.getBody().replace("\n", "\\n");
					sb.append(String.format("note %s of %s: %s\n", position, ne.getName(), body));
				}
			}
		}
	}

	/**
	 * Get the comments for a given element based. These includes owned comments and those
	 * based on inverse references (annotatedElement)
	 * 
	 * @param element a UML element
	 * @return a list of comments
	 */
	public static List<Comment> getComments(Element element) {
		List<Comment> comments = new ArrayList<Comment>();
		// add owned comments, unless they specify annotated elements
		for (Comment ownedComment : element.getOwnedComments()) {
			if (ownedComment.getAnnotatedElements().isEmpty()) {
				comments.add(ownedComment);
			}
		}
		for (Setting setting : UML2Util.getNonNavigableInverseReferences(element)) {
			EObject eObj = setting.getEObject();
			if (eObj instanceof Comment) {
				comments.add((Comment) eObj);
			}
		}
		return comments;
	}
}
