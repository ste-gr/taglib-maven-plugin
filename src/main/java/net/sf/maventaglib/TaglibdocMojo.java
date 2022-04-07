/**
 *
 * Copyright (C) 2004-2014 Fabrizio Giustina
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.sf.maventaglib;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;

import com.sun.tlddoc.TLDDocGenerator;


/**
 * Generates taglibdoc documentation.
 * @goal taglibdoc
 * @author Fabrizio Giustina
 * @version $Id: TaglibdocMojo.java 217 2014-08-15 20:50:32Z fgiust $
 */
public class TaglibdocMojo extends AbstractMavenReport implements MavenReport
{

    /**
     * title for tlddoc generated documentation.
     * @parameter expression="${project.name} Tag library documentation"
     */
    private String title;

    /**
     * TldDoc output dir.
     * @parameter expression="${project.reporting.outputDirectory}/tlddoc"
     */
    private File tldDocDir;

    /**
     * Directory containing tld or tag files. Subdirectories are also processed, unless the "dontRecurseIntoSubdirs"
     * parameter is set.
     * @parameter alias="taglib.src.dir" expression="src/main/resources/META-INF"
     */
    private File srcDir;

    /**
     * If set, only file contained directly in the specified directory are used.
     * @parameter
     */
    private boolean dontRecurseIntoSubdirs;

    /**
     * Site renderer component.
     * @component
     */
    private Renderer siteRenderer;

    /**
     * Directory containing custom xsl files (equivalent to the "-xslt" parameter to tlddoc).
     * @parameter
     */
    private File xsltDir;

    /**
     * @see net.sf.maventaglib.AbstractTaglibMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().debug(MessageFormat.format(Messages.getString("Taglib.generating.tlddoc"), //$NON-NLS-1$
            new Object[]{srcDir.getAbsolutePath() }));
        TLDDocGenerator generator = new TLDDocGenerator();
        generator.setOutputDirectory(tldDocDir);
        generator.setQuiet(true);
        generator.setWindowTitle(this.title);
        if (xsltDir != null)
        {
            generator.setXSLTDirectory(xsltDir);
        }

        String searchprefix = dontRecurseIntoSubdirs ? "" : "**/";

        if (!srcDir.isDirectory())
        {
            throw new MojoExecutionException(MessageFormat.format(
                Messages.getString("Taglib.notadir"), new Object[]{srcDir //$NON-NLS-1$
                    .getAbsolutePath() }));
        }

        try
        {
            // handle tlds
            List tlds = FileUtils.getFiles(srcDir, searchprefix + "*.tld", null); //$NON-NLS-1$
            for (Iterator it = tlds.iterator(); it.hasNext();)
            {
                generator.addTLD((File) it.next());
            }

            // handle tag files. Add any directory containing .tag or .tagx files
            List tags = FileUtils.getFiles(srcDir, searchprefix + "*.tag", null); //$NON-NLS-1$
            tags.addAll(FileUtils.getFiles(srcDir, searchprefix + "*.tagx", null)); //$NON-NLS-1$

            if (!tags.isEmpty())
            {
                Set directories = new HashSet();
                for (Iterator it = tags.iterator(); it.hasNext();)
                {
                    directories.add(((File) it.next()).getParentFile());
                }
                for (Iterator it = directories.iterator(); it.hasNext();)
                {
                    generator.addTagDir((File) it.next());
                }
            }

        }
        catch (IOException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        try
        {
            generator.generate();
        }
        catch (Throwable e)
        {
            getLog().error(MessageFormat.format(Messages.getString("Taglib.exception"), //$NON-NLS-1$
                new Object[]{e.getClass(), e.getMessage() }), e);
        }
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    @Override
    protected String getOutputDirectory()
    {
        return null;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    @Override
    protected MavenProject getProject()
    {
        return null;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException
    {
        try
        {
            execute();
        }
        catch (MojoExecutionException e)
        {
            throw new MavenReportException(e.getMessage(), e);
        }

    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "tlddoc/index"; //$NON-NLS-1$
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName(Locale locale)
    {
        return Messages.getString("Taglibdoc.name"); //$NON-NLS-1$
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription(Locale locale)
    {
        return Messages.getString("Taglibdoc.description"); //$NON-NLS-1$
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#isExternalReport()
     */
    @Override
    public boolean isExternalReport()
    {
        return true;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    @Override
    public boolean canGenerateReport()
    {

        if (!srcDir.isDirectory())
        {
            return false;
        }

        try
        {
            boolean hasTldFiles = FileUtils.getFiles(srcDir, "**/*.tld", null).size() > 0;//$NON-NLS-1$
            boolean hasTagFiles = FileUtils.getFiles(srcDir, "**/*.tag", null).size() > 0;//$NON-NLS-1$
            return hasTldFiles || hasTagFiles;
        }
        catch (IOException e)
        {
            getLog().error(e.getMessage(), e);
        }
        return false;

    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    @Override
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

}