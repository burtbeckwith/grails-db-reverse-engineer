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

import grails.util.GrailsUtil

import org.hibernate.cfg.JDBCMetaDataConfiguration
import org.hibernate.cfg.reveng.ReverseEngineeringSettings
import org.hibernate.tool.hbm2x.Exporter
import org.hibernate.tool.hbm2x.HibernateMappingExporter

/**
 * Main class, called from the reverse engineer script.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class Reenigne {

	File destDir
	String packageName
	String driverClass
	String password
	String username
	String url
	boolean preferBasicCompositeIds = true
	boolean detectOneToOne = true
	boolean detectManyToMany = true
	boolean detectOptimisticLock = true
	boolean ejb3 = false
	boolean jdk5 = true

	GrailsReverseEngineeringStrategy reverseEngineeringStrategy = GrailsReverseEngineeringStrategy.INSTANCE

	private GrailsPojoExporter pojoExporter = new GrailsPojoExporter()
	private HibernateMappingExporter hbmXmlExporter = new HibernateMappingExporter()
	private JDBCMetaDataConfiguration configuration = new JDBCMetaDataConfiguration()
	private Properties properties = new Properties()

	void execute() {
		try {
			buildConfiguration()

			configureExporter pojoExporter
			pojoExporter.getProperties().setProperty('ejb3', ejb3.toString())
			pojoExporter.getProperties().setProperty('jdk5', jdk5.toString())

			configureExporter hbmXmlExporter

			pojoExporter.start()
//			hbmXmlExporter.start()
		}
		catch (e) {
			GrailsUtil.sanitize e
			e.printStackTrace()
			throw e
		}
	}

	private void configureExporter(Exporter exporter) {
		exporter.setProperties properties
		exporter.setConfiguration configuration
		exporter.setOutputDirectory destDir
	}

	private void buildConfiguration() {
		properties.putAll(configuration.getProperties())

		properties.put 'hibernate.connection.driver_class', driverClass
		properties.put 'hibernate.connection.password', password
		properties.put 'hibernate.connection.url', url
		properties.put 'hibernate.connection.username', username
		configuration.setProperties(properties)

		configuration.setPreferBasicCompositeIds preferBasicCompositeIds

		ReverseEngineeringSettings settings = new ReverseEngineeringSettings(reverseEngineeringStrategy)
				.setDefaultPackageName(packageName)
				.setDetectManyToMany(detectManyToMany)
				.setDetectOneToOne(detectOneToOne)
				.setDetectOptimisticLock(detectOptimisticLock)
		reverseEngineeringStrategy.setSettings settings

		configuration.setReverseEngineeringStrategy reverseEngineeringStrategy
		configuration.readFromJDBC()
		configuration.buildMappings()
	}
}
