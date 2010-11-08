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

import java.util.regex.Pattern

import org.apache.log4j.Logger
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy
import org.hibernate.cfg.reveng.TableIdentifier
import org.hibernate.mapping.Table
import org.springframework.util.AntPathMatcher

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GrailsReverseEngineeringStrategy extends DefaultReverseEngineeringStrategy {

	private final Logger log = Logger.getLogger(getClass())

	static final GrailsReverseEngineeringStrategy INSTANCE = new GrailsReverseEngineeringStrategy()

	private Set<String> excludeTables = []
	private Set<Pattern> excludeTableRegexes = []
	private Set<String> excludeTableAntPatterns = []
	private Set<String> includeTables = []
	private Set<Pattern> includeTableRegexes = []
	private Set<String> includeTableAntPatterns = []
	private Map<String, List<String>> excludeColumns = [:]
	private Map<String, List<Pattern>> excludeColumnRegexes = [:]
	private Map<String, List<String>> excludeColumnAntPatterns = [:]
	private Map<String, String> versionColumnNames = [:]
	private Set<String> manyToManyTables = []
	private Set<String> mappedManyToManyTables = []
	private Map<String, String> belongsTos = [:]
	private AntPathMatcher antMatcher = new AntPathMatcher()

	@Override
	boolean excludeTable(TableIdentifier ti) {

		String name = ti.name

		if (includeTables || includeTableRegexes || includeTableAntPatterns) {
			return isNotIncluded(name)
		}

		isExcluded name
	}

	private boolean isNotIncluded(String name) {
		if (!includeTables.contains(name)) {
			log.debug "table $name not included by name"
			return true
		}

		for (Pattern pattern : includeTableRegexes) {
			if (!pattern.matcher(name).matches()) {
				log.debug "table $name not included by regex $pattern"
				return true
			}
		}

		for (String pattern : includeTableAntPatterns) {
			if (!antMatcher.match(pattern, name)) {
				log.debug "table $name not included by pattern $pattern"
				return true
			}
		}

		false
	}

	private boolean isExcluded(String name) {
		if (excludeTables.contains(name)) {
			log.debug "table $name excluded by name"
			return true
		}

		for (Pattern pattern : excludeTableRegexes) {
			if (pattern.matcher(name).matches()) {
				log.debug "table $name excluded by regex $pattern"
				return true
			}
		}

		for (String pattern : excludeTableAntPatterns) {
			if (antMatcher.match(pattern, name)) {
				log.debug "table $name excluded by pattern $pattern"
				return true
			}
		}

		false
	}

	@Override
	boolean excludeColumn(TableIdentifier identifier, String columnName) {

		List<String> excludeNames = excludeColumns[identifier.name]
		if (excludeNames?.contains(columnName)) {
			log.debug "column $columnName in table $identifier.name excluded by name"
			return true
		}

		for (Pattern pattern in excludeColumnRegexes[identifier.name]) {
			if (pattern.matcher(columnName).matches()) {
				log.debug "column $columnName in table $identifier.name excluded by regex $pattern"
				return true
			}
		}

		for (String pattern in excludeColumnAntPatterns[identifier.name]) {
			if (antMatcher.match(pattern, columnName)) {
				log.debug "column $columnName in table $identifier.name excluded by pattern $pattern"
				return true
			}
		}

		false
	}

	@Override
	String getOptimisticLockColumnName(TableIdentifier identifier) {
		String name = versionColumnNames[identifier.name]
		if (name) {
			log.debug "using '$name' for version in table $identifier.name"
		}
		name
	}

	@Override
	boolean isManyToManyTable(Table table) {
		if (mappedManyToManyTables.contains(table.name)) {
			return false
		}

		isReallyManyToManyTable table
	}

	boolean isReallyManyToManyTable(Table table) {
		if (manyToManyTables.contains(table.name)) {
			log.debug "using $table.name as many-to-many table"
			return true
		}
		super.isManyToManyTable(table)
	}

	/**
	 * Register a table name to exclude.
	 * @param name the name
	 */
	void addExcludeTable(String name) { excludeTables << name }

	/**
	 * Register a regex pattern for table names to ignore.
	 * @param pattern the pattern
	 */
	void addExcludeTableRegex(String pattern) { excludeTableRegexes << Pattern.compile(pattern) }

	/**
	 * Register an Ant-style pattern for table names to ignore.
	 * @param pattern the pattern
	 */
	void addExcludeTableAntPattern(String pattern) { excludeTableAntPatterns << pattern }

	/**
	 * Register a table name to include.
	 * @param name the name
	 */
	void addIncludeTable(String name) { includeTables << name }

	/**
	 * Register a regex pattern for table names to include.
	 * @param pattern the pattern
	 */
	void addIncludeTableRegex(String pattern) { includeTableRegexes << Pattern.compile(pattern) }

	/**
	 * Register an Ant-style pattern for table names to include.
	 * @param pattern the pattern
	 */
	void addIncludeTableAntPattern(String pattern) { includeTableAntPatterns << pattern }

	/**
	 * Register a column name to exclude.
	 * @param table the table name
	 * @param name the column name
	 */
	void addExcludeColumn(String table, String name) {
		getOrCreateList(excludeColumns, table) << name
	}

	/**
	 * Register a regex pattern for column names to ignore.
	 * @param table the table name
	 * @param pattern the column name pattern
	 */
	void addExcludeColumnRegex(String table, String pattern) {
		getOrCreateList(excludeColumnRegexes, table) << Pattern.compile(pattern)
	}

	/**
	 * Register an Ant-style pattern for column names to ignore.
	 * @param table the table name
	 * @param pattern the column name pattern
	 */
	void addExcludeColumnAntPattern(String table, String pattern) {
		getOrCreateList(excludeColumnAntPatterns, table) << pattern
	}

	private List getOrCreateList(Map map, String key) {
		List list = map[key]
		if (list == null) {
			list = []
			map[key] = list
		}
		list
	}

	/**
	 * Set the name of the optimistic lock version column if it's different from 'version'.
	 * @param table the table name
	 * @param column the column name
	 */
	void addVersionColumn(String table, String column) {
		versionColumnNames[table] = column
	}

	/**
	 * Add a table name that should be considered a many-to-many join table;
	 * useful if the table has more properties than the two foreign keys since
	 * otherwise Hibernate will ignore it.
	 *
	 * @param name the table name
	 */
	void addManyToManyTable(String name) {
		manyToManyTables << name
	}

	/**
	 * Add a table name that should be mapped as a domain class rather than creating
	 * standard hasMany/belongsTo.
	 *
	 * @param name the table name
	 */
	void addMappedManyToManyTable(String name) {
		mappedManyToManyTables << name
	}

	/**
	 * Register the 'belongsTo' end of a many-to-many; needs to be set since it otherwise
	 * can't be inferred.
	 * @param joinTable the database join table name
	 * @param name the table name of the 'owned' domain class
	 */
	void setManyToManyBelongsTo(String joinTable, String name) {
		belongsTos[joinTable] = name
	}

	boolean isManyToManyBelongsTo(Table joinTable, Table table) {
		table.name.equals(belongsTos[joinTable.name])
	}
}
