${pojo.getPackageDeclaration()}
${pojo.findNewProperties()}
<#assign classbody>
${pojo.renderClassStart()}

${pojo.renderProperties()}

${pojo.renderHashCodeAndEquals()}

${pojo.renderMany()}

${pojo.renderMapping()}

${pojo.renderConstraints()}
}
</#assign>

${pojo.generateImports()}${classbody}