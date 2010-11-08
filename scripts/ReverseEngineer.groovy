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

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

includeTargets << grailsScript('_GrailsBootstrap')

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
target(reverseEngineer: 'Reverse-engineers a database and creates domain classes') {
	depends(packageApp, loadApp)

	createConfig()
	def dsConfig = CH.config.dataSource

	def reenigne = classLoader.loadClass('grails.plugin.reveng.Reenigne').newInstance()
	reenigne.driverClass = dsConfig.driverClassName ?: 'org.hsqldb.jdbcDriver' // 'org.h2.Driver'
	reenigne.password = dsConfig.password ?: ''
	reenigne.username = dsConfig.username ?: 'sa'
	reenigne.url = dsConfig.url ?: 'jdbc:hsqldb:mem:testDB' // 'jdbc:h2:mem:testDB'

	def revengConfig = CH.config.grails.plugin.reveng
	reenigne.packageName = revengConfig.packageName ?: metadata['app.name']
	reenigne.destDir = new File(basedir, revengConfig.destDir ?: 'grails-app/domain')

	def strategy = reenigne.reverseEngineeringStrategy

	revengConfig.versionColumns.each { table, column -> strategy.addVersionColumn table, column }

	revengConfig.manyToManyTables.each { table -> strategy.addManyToManyTable table }

	revengConfig.manyToManyBelongsTos.each { manyTable, belongsTable -> strategy.setManyToManyBelongsTo manyTable, belongsTable }

	revengConfig.excludeTables.each { table -> strategy.addExcludeTable table }

	revengConfig.excludeTableRegexes.each { pattern -> strategy.addExcludeTableRegex pattern }

	revengConfig.excludeTableAntPatterns.each { pattern -> strategy.addExcludeTableAntPattern pattern }

	revengConfig.excludeColumns.each { table, column -> strategy.addExcludeColumn table, column }

	revengConfig.excludeColumnRegexes.each { table, pattern -> strategy.addExcludeColumnRegex table, pattern }

	revengConfig.excludeColumnAntPatterns.each { table, pattern -> strategy.addExcludeColumnAntPattern table, pattern }

	revengConfig.mappedManyToManyTable.each { table -> strategy.addMappedManyToManyTable table }

	ant.echo message: "Starting reverse engineering, connecting to '$reenigne.url' as '$reenigne.username' ..."
	reenigne.execute()
	ant.echo message: 'Finished reverse engineering'
}

setDefaultTarget reverseEngineer
