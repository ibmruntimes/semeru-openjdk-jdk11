/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * ===========================================================================
 * (c) Copyright IBM Corp. 2022, 2022 All Rights Reserved
 * ===========================================================================
 */

package jdk.javadoc.internal.doclets.formats.html;

import java.util.SortedSet;

import javax.lang.model.element.PackageElement;

import jdk.javadoc.internal.doclets.formats.html.markup.HtmlConstants;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTag;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Navigation;
import jdk.javadoc.internal.doclets.formats.html.markup.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.StringContent;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;

/**
 * Generate Class Hierarchy page for all the Classes in this run.  Use
 * ClassTree for building the Tree. The name of
 * the generated file is "overview-tree.html" and it is generated in the
 * current or the destination directory.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @author Atul M Dambalkar
 * @author Bhavesh Patel (Modified)
 */
public class TreeWriter extends AbstractTreeWriter {

    /**
     * Packages in this run.
     */
    SortedSet<PackageElement> packages;

    /**
     * True if there are no packages specified on the command line,
     * False otherwise.
     */
    private final boolean classesOnly;

    private final Navigation navBar;

    /**
     * Constructor to construct TreeWriter object.
     *
     * @param configuration the current configuration of the doclet.
     * @param filename String filename
     * @param classtree the tree being built.
     */
    public TreeWriter(HtmlConfiguration configuration, DocPath filename, ClassTree classtree) {
        super(configuration, filename, classtree);
        packages = configuration.packages;
        classesOnly = packages.isEmpty();
        this.navBar = new Navigation(null, configuration, fixedNavDiv, PageMode.TREE, path);
    }

    /**
     * Create a TreeWriter object and use it to generate the
     * "overview-tree.html" file.
     *
     * @param configuration the configuration for this doclet
     * @param classtree the class tree being documented.
     * @throws  DocFileIOException if there is a problem generating the overview tree page
     */
    public static void generate(HtmlConfiguration configuration,
                                ClassTree classtree) throws DocFileIOException {
        DocPath filename = DocPaths.OVERVIEW_TREE;
        TreeWriter treegen = new TreeWriter(configuration, filename, classtree);
        treegen.generateTreeFile();
    }

    /**
     * Generate the interface hierarchy and class hierarchy.
     *
     * @throws DocFileIOException if there is a problem generating the overview tree page
     */
    public void generateTreeFile() throws DocFileIOException {
        HtmlTree body = getTreeHeader();
        Content headContent = contents.hierarchyForAllPackages;
        Content heading = HtmlTree.HEADING(HtmlConstants.TITLE_HEADING, false,
                HtmlStyle.title, headContent);
        Content div = HtmlTree.DIV(HtmlStyle.header, heading);
        addPackageTreeLinks(div);
        HtmlTree htmlTree = (configuration.allowTag(HtmlTag.MAIN))
                ? HtmlTree.MAIN()
                : body;
        htmlTree.add(div);
        HtmlTree divTree = new HtmlTree(HtmlTag.DIV);
        divTree.setStyle(HtmlStyle.contentContainer);
        addTree(classtree.baseClasses(), "doclet.Class_Hierarchy", divTree);
        addTree(classtree.baseInterfaces(), "doclet.Interface_Hierarchy", divTree);
        addTree(classtree.baseAnnotationTypes(), "doclet.Annotation_Type_Hierarchy", divTree);
        addTree(classtree.baseEnums(), "doclet.Enum_Hierarchy", divTree, true);
        htmlTree.add(divTree);
        if (configuration.allowTag(HtmlTag.MAIN)) {
            body.add(htmlTree);
        }
        if (configuration.allowTag(HtmlTag.FOOTER)) {
            htmlTree = HtmlTree.FOOTER();
        } else {
            htmlTree = body;
        }
        addBottom(htmlTree);
        if (configuration.allowTag(HtmlTag.FOOTER)) {
            body.add(htmlTree);
        }
        printHtmlDocument(null, true, body);
    }

    /**
     * Add the links to all the package tree files.
     *
     * @param contentTree the content tree to which the links will be added
     */
    protected void addPackageTreeLinks(Content contentTree) {
        //Do nothing if only unnamed package is used
        if (isUnnamedPackage()) {
            return;
        }
        if (!classesOnly) {
            Content span = HtmlTree.SPAN(HtmlStyle.packageHierarchyLabel,
                    contents.packageHierarchies);
            contentTree.add(span);
            HtmlTree ul = new HtmlTree(HtmlTag.UL);
            ul.setStyle(HtmlStyle.horizontal);
            int i = 0;
            for (PackageElement pkg : packages) {
                // If the package name length is 0 or if -nodeprecated option
                // is set and the package is marked as deprecated, do not include
                // the page in the list of package hierarchies.
                if (pkg.isUnnamed() ||
                        (configuration.nodeprecated && utils.isDeprecated(pkg))) {
                    i++;
                    continue;
                }
                DocPath link = pathString(pkg, DocPaths.PACKAGE_TREE);
                Content li = HtmlTree.LI(links.createLink(link,
                        new StringContent(utils.getPackageName(pkg))));
                if (i < packages.size() - 1) {
                    li.add(", ");
                }
                ul.add(li);
                i++;
            }
            contentTree.add(ul);
        }
    }

    /**
     * Get the tree header.
     *
     * @return a content tree for the tree header
     */
    protected HtmlTree getTreeHeader() {
        String title = configuration.getText("doclet.Window_Class_Hierarchy");
        HtmlTree bodyTree = getBody(true, getWindowTitle(title));
        HtmlTree htmlTree = (configuration.allowTag(HtmlTag.HEADER))
                ? HtmlTree.HEADER()
                : bodyTree;
        addTop(htmlTree);
        navBar.setUserHeader(getUserHeaderFooter(true));
        htmlTree.add(navBar.getContent(true));
        if (configuration.allowTag(HtmlTag.HEADER)) {
            bodyTree.add(htmlTree);
        }
        return bodyTree;
    }

    private boolean isUnnamedPackage() {
        return packages.size() == 1 && packages.first().isUnnamed();
    }
}
