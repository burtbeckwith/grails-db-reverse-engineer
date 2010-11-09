/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.reveng

import org.apache.log4j.Logger
import org.hibernate.tool.hbm2x.ArtifactCollector
import org.hibernate.tool.hbm2x.ExporterException
import org.hibernate.tool.hbm2x.TemplateHelper
import org.hibernate.tool.hbm2x.TemplateProducer

/**
 * Doesn't overwrite existing classes if configured not to.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GrailsTemplateProducer extends TemplateProducer {

	private Logger log = Logger.getLogger(getClass())
	private TemplateHelper templateHelper
	private ArtifactCollector artifactCollector
	private boolean overwrite

	GrailsTemplateProducer(TemplateHelper templateHelper, ArtifactCollector artifactCollector,
			boolean overwrite) {
		super(templateHelper, artifactCollector)
		this.templateHelper = templateHelper
		this.artifactCollector = artifactCollector
		this.overwrite = overwrite
	}

	@Override
	void produce(Map additionalContext, String templateName, File destination,
			String identifier, String fileType, String rootContext) {

		if (!overwrite && destination.exists()) {
			log.warn "Not overwriting $destination"
			return
		}

		String content = produceToString(additionalContext, templateName, rootContext)
		if (!content.trim()) {
			log.warn "Generated output is empty. Skipped creation for file $destination"
			return
		}

		FileWriter fileWriter
		try {
			templateHelper.ensureExistence destination
			artifactCollector.addFile destination, fileType
			log.debug "Creating $destination.absolutePath"
			fileWriter = new FileWriter(destination)
			fileWriter.write content
		}
		catch (e) {
			throw new ExporterException('Error while writing result to file', e)
		}
		finally {
			try {
				fileWriter?.flush()
				fileWriter?.close()
			}
			catch (IOException e) {
				log.warn "Exception while flushing/closing $destination", e
			}
		}
	}

	private String produceToString(Map additionalContext, String templateName, String rootContext) {
		additionalContext.each { k, v -> templateHelper.putInContext k, v }

		StringWriter writer = new StringWriter()
		BufferedWriter bufferedWriter = new BufferedWriter(writer)
		templateHelper.processTemplate(templateName, bufferedWriter, rootContext)

		additionalContext.each { k, v -> templateHelper.removeFromContext k, v }

		try {
			bufferedWriter.flush()
		}
		catch (IOException e) {
			throw new RuntimeException('Error while flushing to string', e)
		}

		writer.toString()
	}
}
