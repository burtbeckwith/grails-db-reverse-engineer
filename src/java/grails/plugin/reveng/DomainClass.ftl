${pojo.getPackageDeclaration()}
<#assign classbody>
class ${pojo.getDeclarationName()}${pojo.renderImplements()}{

<#foreach prop in pojo.getAllPropertiesIterator()><#if pojo.getMetaAttribAsBool(prop, "gen-property", true)>
	${pojo.getJavaTypeName(prop, jdk5)} ${prop.name}</#if>
</#foreach>
<#if pojo.needsEqualsHashCode()>${pojo.renderHashCodeAndEquals()}</#if>
${pojo.renderMany()}

	static mapping = {
${pojo.renderId()}${pojo.renderVersion()}${pojo.renderTable()}	}

	static constraints = {
${pojo.renderConstraints()}	}
}
</#assign>

${pojo.generateImports()}${classbody}