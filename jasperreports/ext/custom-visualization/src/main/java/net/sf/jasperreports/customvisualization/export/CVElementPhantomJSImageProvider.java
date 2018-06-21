/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2018 TIBCO Software Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.customvisualization.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jasperreports.customvisualization.CVPrintElement;
import net.sf.jasperreports.customvisualization.CVUtils;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.repo.RepositoryUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Giulio Toffoli (gtoffoli@tibco.com)
 */
public class CVElementPhantomJSImageProvider extends CVElementImageProvider
{
	private static final Log log = LogFactory.getLog(CVElementPhantomJSImageProvider.class);
	private static final String CVC_RESOURCE_PREFIX = "jr_cv_";

	private static final String PHANTOMJS_COMPONENT_TEMPLATE = "net/sf/jasperreports/customvisualization/templates/phantomjs_component.vm";

	public static final String PROPERTY_PHANTOMJS_PREFIX = "com.jaspersoft.jasperreports.components.customvisualization.phantomjs.";

	public static final String PROPERTY_PHANTOMJS_EXECUTABLE_PATH = PROPERTY_PHANTOMJS_PREFIX + "executable.path";
	public static final String PROPERTY_PHANTOMJS_EXECUTABLE_TIMEOUT = PROPERTY_PHANTOMJS_PREFIX + "executable.timeout";
	public static final String PROPERTY_PHANTOMJS_TEMPDIR_PATH = PROPERTY_PHANTOMJS_PREFIX + "tempdir.path";
	public static final String PROPERTY_PHANTOMJS_DEBUG = PROPERTY_PHANTOMJS_PREFIX + "debug";

	private final String[] scriptResourceLocations = new String[] {
			"net/sf/jasperreports/customvisualization/scripts/div2svg.js",
			"net/sf/jasperreports/customvisualization/resources/require/require.js",
			"net/sf/jasperreports/customvisualization/resources/require/cv-component.js"
	};

	private ConcurrentHashMap<String, String> commonScripts = new ConcurrentHashMap<>(16, 0.75f, 1);


	/**
	 * Returns the location of a newly created image,
	 * 
	 * @param jasperReportsContext
	 * @param element
	 * @return image byte array
	 * @throws Exception
	 */
	@Override
	public byte[] getImageData(
		JasperReportsContext jasperReportsContext, 
		JRGenericPrintElement element) throws Exception
	{
		if (element.getParameterValue(CVPrintElement.CONFIGURATION) == null)
		{
			throw new JRRuntimeException("Configuration object is null.");
		}

		String phantomjsExecutablePath = jasperReportsContext.getProperty(CVElementPhantomJSImageProvider.PROPERTY_PHANTOMJS_EXECUTABLE_PATH);
		if (phantomjsExecutablePath == null)
		{
			phantomjsExecutablePath = "phantomjs";
		}

		int phantomjsTimeout;
		String timeoutProperty = jasperReportsContext.getProperty(CVElementPhantomJSImageProvider.PROPERTY_PHANTOMJS_EXECUTABLE_TIMEOUT);
		if (timeoutProperty != null)
		{
			phantomjsTimeout = Integer.parseInt(timeoutProperty);
		}
		else
		{
			phantomjsTimeout = 60000;
		}

		String phantomjsTempFolderPath = jasperReportsContext.getProperty(CVElementPhantomJSImageProvider.PROPERTY_PHANTOMJS_TEMPDIR_PATH);
		if (phantomjsTempFolderPath == null)
		{
			phantomjsTempFolderPath = System.getProperty("java.io.tmpdir");
		}

		File tempFolder = new File(phantomjsTempFolderPath);
		if (tempFolder.exists())
		{
			// Collect the files that must be deleted as we finish to render everything
			List<File> cleanableResourcePaths = new ArrayList<>();

			try
			{
				List<String> scripts = new ArrayList<>();
				RepositoryUtil repositoryUtil = RepositoryUtil.getInstance(jasperReportsContext);

				for (String scriptLocation: scriptResourceLocations) {
					scripts.add(copyResourceToTempFolder(scriptLocation, tempFolder, cleanableResourcePaths,
							true, repositoryUtil));
				}

				scripts.add(copyResourceToTempFolder((String)element.getParameterValue(CVPrintElement.SCRIPT_URI),
						tempFolder, cleanableResourcePaths, true, repositoryUtil));

				String cssUriParameter = (String)element.getParameterValue(CVPrintElement.CSS_URI);
				String cssUri = null;
				if (cssUriParameter != null) {
					cssUri= copyResourceToTempFolder(cssUriParameter, tempFolder, cleanableResourcePaths,
							true, repositoryUtil);
				}

				String htmlPage = getHtmlPage(
						jasperReportsContext,
						element,
						scripts.subList(1, scripts.size()),
						cssUri);

				File htmlPageFile = createTempFile("in.html", tempFolder, cleanableResourcePaths, false);
				try (InputStream is = new ByteArrayInputStream(htmlPage.getBytes());
					 OutputStream os = new FileOutputStream(htmlPageFile)) {
					CVUtils.byteStreamCopy(is, os);
				}

				File outputSvgFile = createTempFile("out.svg", tempFolder, cleanableResourcePaths, false);
				boolean renderAsPng = CVUtils.isRenderAsPng(element);

				try
				{
					runCommand(new String[] {
							phantomjsExecutablePath,
							scripts.get(0),
							"--output-format=" + (renderAsPng ? "png" : "svg"),
							"--timeout=" + CVUtils.getTimeout(element),
                            "--zoom-factor=" + CVUtils.getZoomFactor(element),
							htmlPageFile.getName(),
							outputSvgFile.getName()
							},
						tempFolder, phantomjsTimeout);
				}
				catch (Exception ex)
				{
					throw new JRRuntimeException(
							"Error while executing the javascript file to generate the SVG image: " + ex.getMessage());
				}

				if (!outputSvgFile.exists() || outputSvgFile.length() <= 0)
				{
					throw new JRRuntimeException(
							"Error while executing the javascript file to generate the SVG image.");
				}

				try (InputStream is = new FileInputStream(outputSvgFile);
					 ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					CVUtils.byteStreamCopy(is, os);

					return os.toByteArray();
				}

			}
			finally
			{
				for (File cleanableResource : cleanableResourcePaths)
				{
					if (cleanableResource.exists() && cleanableResource.canWrite())
					{
						JRPropertiesUtil propertiesUtil = JRPropertiesUtil.getInstance(jasperReportsContext);
						boolean isPhantomJSinDebugMode = propertiesUtil.getBooleanProperty(CVElementPhantomJSImageProvider.PROPERTY_PHANTOMJS_DEBUG, false);

						String keepTempFilesProperty = element.getPropertiesMap().getProperty("cv.keepTemporaryFiles");
						boolean keepTempFiles = keepTempFilesProperty != null && keepTempFilesProperty.equals("true");

						if (!(isPhantomJSinDebugMode && keepTempFiles)) {
							if (log.isDebugEnabled())
							{
								log.debug("Cleaning up resource after rendering of element " + element.hashCode() + ": "
										+ cleanableResource.getAbsolutePath());
							}

							cleanableResource.delete();
						}
					}
				}
			}

		}
		else
		{
			throw new JRRuntimeException("Temp folder '" + tempFolder + "' does not exist!");
		}
	}

	public String getHtmlPage(
			JasperReportsContext jrContext,
			JRGenericPrintElement element,
			List<String> scripts,
			String cssUri
	)
	{
		Map<String, Object> originalConfiguration = (Map<String, Object>) element.getParameterValue(CVPrintElement.CONFIGURATION);

		if (originalConfiguration == null)
		{
			if (log.isWarnEnabled())
			{
				log.warn("Configuration object in the element " + element + " is NULL!");
			}
			throw new JRRuntimeException("Configuration object in the element " + element + " is NULL!");
		}

		// Duplicate the configuration.
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.putAll(originalConfiguration);

		ObjectMapper mapper = new ObjectMapper();
		try
		{
			Map<String, Object> jsonConfiguration = CVElementJsonHandler.createConfigurationForJSON(configuration, null);
			jsonConfiguration.put("animation", false);

			String instanceData = mapper.writeValueAsString(jsonConfiguration);
			configuration.put("instanceData", instanceData);
		}
		catch (Exception ex)
		{
			if (log.isWarnEnabled())
			{
				log.warn("Error dumping the JSON for the configuration...: " + ex.getMessage(), ex);
			}
			throw new JRRuntimeException("Error dumping the JSON for the configuration...: " + ex.getMessage());
		}

		configuration.put("element", element);

		Map<String, Object> velocityContext = new HashMap<>();
		velocityContext.put("scripts", scripts);
		velocityContext.put("configuration", configuration);
		velocityContext.put("module", element.getParameterValue(CVPrintElement.MODULE));
		velocityContext.put("cssUri", cssUri);

        return CVUtils.fillVelocityTemplate(jrContext, PHANTOMJS_COMPONENT_TEMPLATE, velocityContext);
	}

	protected String copyResourceToTempFolder(String resourceLocation,
											  File tempFolder, List<File> cleanableResources,
											  boolean isCommonResource, RepositoryUtil repositoryUtil) {
		String tempResourceName = commonScripts.get(resourceLocation);

		if (tempResourceName == null) {
			try {
				String resourceName = CVUtils.getResourceName(resourceLocation);
				File tempFile = createTempFile(resourceName, tempFolder, cleanableResources, isCommonResource);

				if (log.isDebugEnabled()) {
					log.debug("Copying " + resourceLocation + " to " + tempFile);
				}

				try (InputStream is = repositoryUtil.getInputStreamFromLocation(resourceLocation);
					 OutputStream os = new FileOutputStream(tempFile)) {
					CVUtils.byteStreamCopy(is, os);
				}

				tempResourceName = tempFile.getName();
				commonScripts.put(resourceLocation, tempResourceName);

			} catch (IOException | JRException e) {
				throw new JRRuntimeException(e);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Serving cached tempResourceName: " + tempResourceName + " for " + resourceLocation);
			}
		}

		return tempResourceName;
	}

	protected File createTempFile(String name, File tempFolder, List<File> cleanableResources, boolean isCommonResource) throws IOException {
		File tempFile = File.createTempFile(CVC_RESOURCE_PREFIX, "_" + name, tempFolder);
		if (isCommonResource) {
			tempFile.deleteOnExit();
		} else {
			cleanableResources.add(tempFile);
		}

		return tempFile;
	}

	/**
	 * Executes a command within the given timeout.
	 * 
	 * @param args
	 * @param currentDirectory
	 * @param timeout
	 */
	private static void runCommand(String[] args, File currentDirectory, final int timeout)
	{
		Thread loggingThread = null;
		Thread interruptingThread = null;

		try
		{
			String cmd = "";
			for (String arg : args)
			{
				cmd += " " + arg;
			}

			if (log.isDebugEnabled())
			{
				log.debug("Executing external command: " + cmd);
			}
			//System.out.println(cmd);

			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(args));
			pb.directory(currentDirectory);

			final Process externalProcess = pb.start();
			final StringBuilder processOutput = new StringBuilder();

			final boolean[] success = new boolean[1];
			success[0] = false;

			loggingThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					BufferedReader br = null;
					try
					{
						br = new BufferedReader(new InputStreamReader(externalProcess.getInputStream()));
						String line;
						while ((line = br.readLine()) != null)
						{
							processOutput.append(line).append("\n");

							if (line.indexOf("SCRIPT_SUCCESS") >= 0)
							{
								success[0] = true;
								killProcess(externalProcess, 100);
							}
							else if (line.indexOf("SCRIPT_ERROR") >= 0)
							{
								success[0] = false;
								killProcess(externalProcess, 100);
							}
						}

                                                if (log.isDebugEnabled())
						{
							log.debug("External process output:\n" + processOutput.toString());
						}
					}
					catch (IOException e)
					{
						if (log.isDebugEnabled())
						{
							log.debug(e.getMessage());
						}
					}
					finally
					{
						if (br != null)
						{
							try
							{
								br.close();
							}
							catch (IOException e)
							{
								if (log.isWarnEnabled())
								{
									log.warn("Failed to close phantomjs process' inputstream", e);
								}
							}
						}
					}
				}
			});

			interruptingThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					if (killProcess(externalProcess, timeout))
					{
						success[0] = false;
					}
				}

			});
			loggingThread.start();
			interruptingThread.start();
			externalProcess.waitFor();

			// We should not care if the phantomjs process does not end on time if it succeeds in producing the desired output.
			if (externalProcess.exitValue() != 0 && !success[0])
			{
				// FIXME we should do loggingThread.join(millis) because the
				// process might end before its output if fully processed

				throw new JRRuntimeException(
						"External process did not end properly; exit value: " + externalProcess.exitValue()
								+ (processOutput.length() > 0 ? "; process output:\n" + processOutput + "\n" : "."));
			}

		}
		catch (IOException e)
		{
			throw new JRRuntimeException(e);
		}
		catch (InterruptedException e)
		{
			throw new JRRuntimeException(e);
		}
		finally
		{

			if (interruptingThread != null && interruptingThread.isAlive())
			{
				try
				{
					interruptingThread.interrupt();
				}
				catch (Exception ex)
				{
				}
			}
			if (loggingThread != null && loggingThread.isAlive())
			{
				try
				{
					loggingThread.interrupt();
				}
				catch (Exception ex)
				{
				}
			}
		}
	}

	/**
	 * Kill a process, if still active, after millisDelay
	 * 
	 * @param externalProcess
	 * @param millisDelay
	 * @return true if the process had to be terminated, false if the process
	 *         exited before the timeout
	 */
	public static boolean killProcess(Process externalProcess, int millisDelay)
	{
		try
		{
			Thread.sleep(millisDelay);
		}
		catch (InterruptedException e)
		{
			// e.printStackTrace();
		}

		try
		{
			int exitValue = externalProcess.exitValue();
			if (log.isDebugEnabled())
			{
				log.debug("External Process monitoring thread - exit value: " + exitValue);
			}
			return false;
		}
		catch (IllegalThreadStateException e)
		{
			if (log.isDebugEnabled())
			{
				log.debug("External Process monitoring thread - destroying process");
			}
			externalProcess.destroy();
			return true;
		}
	}
}
