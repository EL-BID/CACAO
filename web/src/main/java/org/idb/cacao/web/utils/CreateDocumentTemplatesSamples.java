package org.idb.cacao.web.utils;

import java.util.ArrayList;
import java.util.List;

import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldType;

public class CreateDocumentTemplatesSamples {

	public static List<DocumentTemplate> getSampleTemplates() {
		
		List<DocumentTemplate> toRet = new ArrayList<>(3);
		
		//Livro diário
		DocumentTemplate docTemplate = new DocumentTemplate();
		docTemplate.setName("DIARIO");
		docTemplate.setGroup("CONTABILIDADE");
		docTemplate.setPeriodicity(Periodicity.MONTHLY);
		docTemplate.setRequired(true);
		docTemplate.setVersion("1.0");
		
		toRet.add(docTemplate);
		List<DocumentField> fields = new ArrayList<>();
		
		DocumentField field = new DocumentField();
		field.setFieldName("Data");
		field.setFieldType(FieldType.DATE);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Data do lançamento");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Código Conta");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Código da conta");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Histórico");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(false);
		field.setDescription("Histórico do lançamento");
		fields.add(field);		
		
		field = new DocumentField();
		field.setFieldName("Valor");
		field.setFieldType(FieldType.DECIMAL);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Valor do lançamento");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("D/C");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Indicador de débito/crédito");
		fields.add(field);
		
		docTemplate.setFields(fields);
		
		//Plano de contas
		docTemplate = new DocumentTemplate();
		docTemplate.setName("PLANO DE CONTAS");
		docTemplate.setGroup("CONTABILIDADE");
		docTemplate.setPeriodicity(Periodicity.MONTHLY);
		docTemplate.setRequired(true);
		docTemplate.setVersion("1.0");
		
		toRet.add(docTemplate);
		fields = new ArrayList<>();		
		
		field = new DocumentField();
		field.setFieldName("Código Conta");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Código da conta");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Nome Conta");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Nome da conta");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Nível");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(false);
		field.setDescription("Nível hierárquico da conta");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Analítica/Sintética");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(false);
		field.setDescription("Indica se a conta é analítica ou sintética");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Centro de custos");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(false);
		field.setDescription("Centro de custos ao qual está atrelada a conta");
		fields.add(field);		
		
		docTemplate.setFields(fields);
		
		//Saldos Iniciais
		docTemplate = new DocumentTemplate();
		docTemplate.setName("SALDOS INICIAIS");
		docTemplate.setGroup("CONTABILIDADE");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(true);
		docTemplate.setVersion("1.0");
		
		toRet.add(docTemplate);
		fields = new ArrayList<>();		
		
		field = new DocumentField();
		field.setFieldName("Código Conta");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Código da conta");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("Saldo Inicial");
		field.setFieldType(FieldType.DECIMAL);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Saldo da conta no início do período");
		fields.add(field);
		
		field = new DocumentField();
		field.setFieldName("D/C");
		field.setFieldType(FieldType.CHARACTER);
		field.setPersonalData(false);
		field.setRequired(true);
		field.setDescription("Indicador do tipo de saldo devedor/credor");
		fields.add(field);		
		
		field = new DocumentField();
		field.setFieldName("Data");
		field.setFieldType(FieldType.DATE);
		field.setPersonalData(false);
		field.setRequired(false);
		field.setDescription("Data do saldo");
		fields.add(field);		
		
		docTemplate.setFields(fields);		
		
		return toRet;
		
	}
	
}
