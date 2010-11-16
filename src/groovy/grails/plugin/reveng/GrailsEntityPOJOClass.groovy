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

import grails.util.GrailsNameUtils

import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import org.hibernate.mapping.Column
import org.hibernate.mapping.ForeignKey
import org.hibernate.mapping.ManyToOne
import org.hibernate.mapping.PersistentClass
import org.hibernate.mapping.Property
import org.hibernate.mapping.Table
import org.hibernate.mapping.UniqueKey
import org.hibernate.mapping.Value
import org.hibernate.tool.hbm2x.Cfg2HbmTool
import org.hibernate.tool.hbm2x.Cfg2JavaTool
import org.hibernate.tool.hbm2x.pojo.EntityPOJOClass
import org.hibernate.type.IntegerType
import org.hibernate.type.LongType

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GrailsEntityPOJOClass extends EntityPOJOClass {

	private PersistentClass clazz
	private Cfg2HbmTool c2h
	private Configuration configuration
	private ConfigObject grailsConfig
	private String newline = System.getProperty('line.separator')
	private newProperties = []

	GrailsEntityPOJOClass(PersistentClass clazz, Cfg2JavaTool cfg, Cfg2HbmTool c2h,
			Configuration configuration, ConfigObject grailsConfig) {
		super(clazz, cfg)
		this.clazz = clazz
		this.c2h = c2h
		this.configuration = configuration
		this.grailsConfig = grailsConfig
	}

	@Override
	String getPackageDeclaration() { super.getPackageDeclaration() - ';' }

	@Override
	Iterator getAllPropertiesIterator() {
		def props = []
		def idProperty = getIdentifierProperty()
		def versionProperty = getVersionProperty()

		super.getAllPropertiesIterator().each {
			if (it == versionProperty) {
				return
			}

			if (it == idProperty) {
				if (c2j.isComponent(it)) {
					it.value.propertyIterator.each { idProp ->
						props << idProp
					}
					return
				}
				if (it.name == 'id' || it.type instanceof LongType || it.type instanceof IntegerType) {
					return
				}
			}

			if (it.value instanceof ManyToOne || it.value instanceof org.hibernate.mapping.Set) {
				return
			}

			props << it
		}
		props.iterator()
	}

	String renderId() {
		def idProperty = getIdentifierProperty()
		if (c2j.isComponent(idProperty)) {
			def idDef = new StringBuilder('\t\tid composite: [')
			String delimiter = ''
			idProperty.value.propertyIterator.each {
				idDef.append delimiter
				idDef.append "\"${findRealIdName(it)}\""
				delimiter = ', '
			}
			idDef.append ']'
			idDef.append newline
			return idDef.toString()
		}

		if (idProperty.name != 'id' || idProperty.value.identifierGeneratorStrategy == 'assigned') {
			def idDef = new StringBuilder('\t\tid ')
			String delimiter = ''

			if (idProperty.name != 'id') {
				idDef.append delimiter
				if (idProperty.type instanceof LongType || idProperty.type instanceof IntegerType) {
					String colName = idProperty.columnIterator.next().name
					idDef.append "column: \"$colName\""
				}
				else {
					idDef.append "name: \"$idProperty.name\""
				}
				delimiter = ', '
			}

			if (idProperty.value.identifierGeneratorStrategy == 'assigned') {
				idDef.append delimiter
				idDef.append "generator: \"assigned\""
				delimiter = ', '
			}

			idDef.append newline
			return idDef.toString()
		}

		''
	}

	String renderVersion() {
		if (hasVersionProperty()) {
			if (getVersionProperty().name != 'version') {
				return '\t\tversion "' + getVersionProperty().value.columnIterator.next().name +
						'"' + newline
			}
			return ''
		}

		'\t\tversion false' + newline
	}

	String renderTable() {
//		if (clazz.table.schema || clazz.table.catalog) {
//			def tableDef = new StringBuilder('\t\ttable ')
//			String delimiter = ''
//
//			if (clazz.table.schema) {
//				tableDef.append delimiter
//				tableDef.append "schema: \"$clazz.table.quotedSchema\""
//				delimiter = ', '
//			}
//
//			if (clazz.table.catalog) {
//				tableDef.append delimiter
//				tableDef.append "catalog: \"$clazz.table.catalog\""
//				delimiter = ', '
//			}
//
//			tableDef.append newline
//			return tableDef.toString()
//		}

		''
	}

	@Override
	String generateImports() {
		def fixed = new StringBuilder()
		String imports = super.generateImports().trim()
		String delimiter = ''
		imports.eachLine { String line ->
			line -= ';'
			if (isValidImport(line - 'import ')) {
				fixed.append delimiter
				fixed.append line
				delimiter = newline
			}
		}

		if (needsEqualsHashCode()) {
			fixed.append delimiter
			fixed.append 'import org.apache.commons.lang.builder.EqualsBuilder'
			delimiter = newline
			fixed.append delimiter
			fixed.append 'import org.apache.commons.lang.builder.HashCodeBuilder'
			fixed.append delimiter
		}

		imports = fixed.toString()
		if (imports) {
			return imports + newline + newline
		}

		''
	}

	private boolean isValidImport(String candidate) {
		if ('java.math.BigDecimal'.equals(candidate) ||
		    'java.math.BigInteger'.equals(candidate)) {
			return false
		}

		if (isInPackage(candidate, 'java.io') ||
		    isInPackage(candidate, 'java.net') ||
		    isInPackage(candidate, 'java.util')) {
			return false
		}

		true
	}

	private boolean isInPackage(String candidate, String pkg) {
		if (!candidate.contains(pkg)) {
			return false
		}

		int index = candidate.lastIndexOf('.')
		if (index == -1) {
			return false
		}

		String candidatePackage = candidate[0..index - 1]
		candidatePackage == pkg
	}

	String renderHashCodeAndEquals() {
		if (!needsEqualsHashCode()) {
			return ''
		}

		def hashCodeDef = new StringBuilder('\tint hashCode() {')
		hashCodeDef.append newline
		hashCodeDef.append '\t\tdef builder = new HashCodeBuilder()'
		hashCodeDef.append newline

		def equalsDef = new StringBuilder('\tboolean equals(other) {')
		equalsDef.append newline
		equalsDef.append '\t\tif (other == null) return false'
		equalsDef.append newline
		equalsDef.append '\t\tdef builder = new EqualsBuilder()'
		equalsDef.append newline

		getAllPropertiesIterator().each { property ->
			if (c2j.getMetaAsBool(property, 'use-in-equals')) {
				String name = findRealIdName(property)
				if (name != property.name) {
					name += '?.id'
				}
				hashCodeDef.append '\t\tbuilder.append '
				hashCodeDef.append name
				hashCodeDef.append newline

				equalsDef.append '\t\tbuilder.append '
				equalsDef.append name
				equalsDef.append ', other.'
				equalsDef.append name
				equalsDef.append newline
			}
		}

		hashCodeDef.append '\t\tbuilder.toHashCode()'
		hashCodeDef.append newline
		hashCodeDef.append '\t}'
		hashCodeDef.append newline

		equalsDef.append '\t\tbuilder.isEquals()'
		equalsDef.append newline
		equalsDef.append '\t}'

		newline + hashCodeDef + newline + equalsDef
	}

	String renderConstraints() {
		def constraints = new StringBuilder()
		getAllPropertiesIterator().each { Property property ->
			if (!getMetaAttribAsBool(property, 'gen-property', true)) {
				return
			}

			def values = [:]
			if (!property.type.isCollectionType() && property.isNullable()) {
				values.nullable = true
			}
			if (property.columnSpan == 1) {
				Column column = property.columnIterator.next()
				if (column.length && column.length != Column.DEFAULT_LENGTH) {
					values.maxSize = column.length
				}

				if (column.scale != 0 && column.scale != Column.DEFAULT_SCALE) {
					values.scale = column.scale
				}

				if (property != getIdentifierProperty() && column.unique) {
					values.unique = true
				}

				clazz.table.uniqueKeyIterator.each { UniqueKey key ->
					if (key.columnSpan == 1 || key.name == clazz.table.primaryKey.name) return
					if (key.columns[-1] == column) {
						def otherNames = key.columns[0..-2].collect { "\"$it.name\"" }
						values.unique = '[' + otherNames.reverse().join(', ') + ']'
					}
				}
			}

			if (values) {
				constraints.append "\t\t$property.name "
				String delimiter = ''
				values.each { k, v ->
					constraints.append delimiter
					constraints.append "$k: $v"
					delimiter = ', '
				}
				constraints.append newline
			}
		}

		constraints.length() ? "\tstatic constraints = {$newline$constraints\t}" : ''
	}

	void findNewProperties() {

		def idProperty = getIdentifierProperty()
		def versionProperty = getVersionProperty()

		super.getAllPropertiesIterator().each {
			if (it == versionProperty || it == idProperty) {
				return
			}

			if (it.value instanceof ManyToOne) {
				newProperties << it
			}
		}
	}

	String renderMany() {

		def revengConfig = grailsConfig.grails.plugin.reveng
		boolean bidirectionalManyToOne = revengConfig.bidirectionalManyToOne instanceof Boolean ?
				revengConfig.bidirectionalManyToOne : true
		boolean mapManyToManyJoinTable = revengConfig.mapManyToManyJoinTable instanceof Boolean ?
				revengConfig.mapManyToManyJoinTable : false

		def idProperty = getIdentifierProperty()
		def versionProperty = getVersionProperty()

		def belongs = []
		def hasMany = []
		def strategy = GrailsReverseEngineeringStrategy.INSTANCE
		super.getAllPropertiesIterator().each { prop ->
			if (prop == versionProperty || prop == idProperty) {
				return
			}

			if (bidirectionalManyToOne && prop.value instanceof ManyToOne) {
				if (!isPartOfPrimaryKey(prop)) {
					belongs << classShortName(prop.value.referencedEntityName)
				}
			}

			if (prop.value instanceof org.hibernate.mapping.Set) {
				boolean isManyToMany = strategy.isManyToManyTable(prop.value.collectionTable)
				boolean isReallyManyToMany = strategy.isReallyManyToManyTable(prop.value.collectionTable)
				if ((bidirectionalManyToOne && !isManyToMany && !isReallyManyToMany) || isManyToMany) {
					String classShortName = classShortName(prop.value.element.type.name)
					hasMany << "$prop.name: $classShortName"
					if (isManyToMany) {
						if (strategy.isManyToManyBelongsTo(prop.value.collectionTable, prop.persistentClass.table)) {
							belongs << findManyToManyOtherSide(prop)
						}
					}
				}
			}
		}

		if (belongs || hasMany) {
			def many = new StringBuilder()
			if (hasMany) {
				many.append combine('\tstatic hasMany = [', ', ', ']', hasMany)
				many.append newline
			}
			if (belongs) {
				many.append combine('\tstatic belongsTo = [', ', ', ']', belongs)
				many.append newline
			}
			return many.toString()
		}

		''
	}

	private boolean isPartOfPrimaryKey(Property prop) {
		if (c2j.isComponent(getIdentifierProperty()) &&
					GrailsReverseEngineeringStrategy.INSTANCE.isReallyManyToManyTable(clazz.table)) {
			String propClassShortName = classShortName(prop.value.referencedEntityName)
			for (newProp in newProperties) {
				if (classShortName(newProp.value.referencedEntityName) == propClassShortName) {
					return true
				}
			}
		}

		false
	}

	private String findRealIdName(Property prop) {
		if (c2j.isComponent(getIdentifierProperty()) &&
					GrailsReverseEngineeringStrategy.INSTANCE.isReallyManyToManyTable(clazz.table)) {
					
			for (newProp in newProperties) {
				if (newProp.name + 'Id' == prop.name) {
					return newProp.name
				}
			}
		}

		prop.name
	}

	String renderProperties() {
		def props = new StringBuilder()
		getAllPropertiesIterator().each { prop ->
			if (getMetaAttribAsBool(prop, 'gen-property', true)) {
				if (findRealIdName(prop) == prop.name) {
					props.append '\t'
					props.append getJavaTypeName(prop, true)
					props.append ' '
					props.append prop.name
					props.append newline
				}
			}
		}

		for (prop in newProperties) {
			props.append '\t'
			props.append classShortName(prop.value.referencedEntityName)
			props.append ' '
			props.append prop.name
			props.append newline
		}

		props.toString()
	}

	String renderMapping() {
		def mapping = new StringBuilder()
		mapping.append renderId()
		mapping.append renderVersion()
		mapping.append renderTable()
		mapping.length() ? "\tstatic mapping = {$newline$mapping\t}" : ''
	}

	String renderClassStart() {
		"class ${getDeclarationName()}${renderImplements()}{"
	}
	
	String renderImplements() {
		getIdentifierProperty().columnSpan > 1 ? ' implements Serializable ' : ' '
	}

	private String findManyToManyOtherSide(Property prop) {
		String belongsTo
		prop.value.collectionTable.foreignKeyIterator.each { ForeignKey fk ->
			if (prop.value.table != fk.referencedTable) {
				belongsTo = classShortName(fk.referencedEntityName)
			}
		}
		belongsTo
	}

	private String classShortName(String className) {
		if (className.indexOf('.') > -1) {
			return className.substring(className.lastIndexOf('.') + 1)
		}
		className
	}

	private String combine(String start, String delim, String end, things) {
		def buffer = new StringBuilder(start)
		String delimiter = ''
		things.each {
			buffer.append delimiter
			buffer.append it
			delimiter = delim
		}
		buffer.append end
		buffer.toString()
	}
}
