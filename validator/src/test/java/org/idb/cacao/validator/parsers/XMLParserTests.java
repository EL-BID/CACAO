package org.idb.cacao.validator.parsers;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountName;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests sample files in XML format with the XMLParser implemented in VALIDATOR
 *
 * @author Leon Silva
 */
@RunWith(JUnitPlatform.class)
public class XMLParserTests {
	/**
	 * Test the sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.xml'
	 * with the column name expression.
	 */
	@Test
	void chartOfAccounts01Test() throws Exception {

		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());

		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XML);
		inputSpec.setInputName("ChartOfAccounts XML");
		template.addInput(inputSpec);

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(TaxPayerId.name())
				.withPathExpression("ChartOfAccounts.TaxPayerId"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(TaxYear.name())
				.withPathExpression("ChartOfAccounts.TaxYear"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(AccountCode.name())
				.withPathExpression("ChartOfAccounts.AccountCode"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(AccountCategory.name())
				.withColumnNameExpression("AccountCategory"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(AccountSubcategory.name())
				.withColumnNameExpression("Subcategory"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(AccountName.name())
				.withColumnNameExpression("AccountName"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName(AccountDescription.name())
				.withColumnNameExpression("Description"));

		byte[] clone = SerializationUtils.serialize(inputSpec);

		DocumentInput inputSpecHierarquical = (DocumentInput) SerializationUtils.deserialize(clone);

		inputSpecHierarquical.getFields().get(2).withPathExpression("ChartOfAccounts.Accounts.Account.AccountCode");
		inputSpecHierarquical.getFields().get(3).withPathExpression("ChartOfAccounts.Accounts.Account.AccountCategory");

		for (String resource : new String[] { /*"/samples/20211411 - Pauls Guitar Shop - Chart of Accounts - IFRS.xml",*/
				"/samples/20211411 - Pauls Guitar Shop - Chart of Accounts - IFRS - hierarquical.xml" }) {

			System.out.println("Testing with: " + resource);

			Resource sampleFile = new ClassPathResource(resource);
			assertTrue(sampleFile.exists());

			try (XMLParser parser = new XMLParser();) {

				parser.setPath(sampleFile.getFile().toPath());
				parser.setDocumentInputSpec(
						resource.equals("/samples/20211411 - Pauls Guitar Shop - Chart of Accounts - IFRS.xml")
								? inputSpec
								: inputSpecHierarquical);
				parser.start();

				try (DataIterator iterator = parser.iterator();) {

					assertTrue(iterator.hasNext(), "Should find the first record");
					Map<String, Object> record = iterator.next();
					assertEquals("123456", toString(record.get(TaxPayerId.name())));
					assertEquals("2021", toString(record.get(TaxYear.name())));
					assertEquals("1.1.1", toString(record.get(AccountCode.name())));
					assertEquals("1", toString(record.get(AccountCategory.name())));
					assertEquals("1.10", toString(record.get(AccountSubcategory.name())));
					assertEquals("Cash", toString(record.get(AccountName.name())));
					assertEquals("Cash and Cash Equivalents", toString(record.get(AccountDescription.name())));

					assertTrue(iterator.hasNext(), "Should find the second record");
					record = iterator.next();
					assertEquals("123456", toString(record.get(TaxPayerId.name())));
					assertEquals("2021", toString(record.get(TaxYear.name())));
					assertEquals("1.2.1", toString(record.get(AccountCode.name())));
					assertEquals("1", toString(record.get(AccountCategory.name())));
					assertEquals("1.9", toString(record.get(AccountSubcategory.name())));
					assertEquals("Accounts Receivable", toString(record.get(AccountName.name())));
					assertEquals("Accounts, Notes And Loans Receivable",
							toString(record.get(AccountDescription.name())));

					assertTrue(iterator.hasNext(), "Should find the third record");
					record = iterator.next();
					assertEquals("123456", toString(record.get(TaxPayerId.name())));
					assertEquals("2021", toString(record.get(TaxYear.name())));
					assertEquals("1.3.1", toString(record.get(AccountCode.name())));
					assertEquals("1", toString(record.get(AccountCategory.name())));
					assertEquals("1.7", toString(record.get(AccountSubcategory.name())));
					assertEquals("Inventory", toString(record.get(AccountName.name())));
					assertEquals("Merchandise in Inventory", toString(record.get(AccountDescription.name())));

					for (int i = 4; i <= 14; i++) {
						assertTrue(iterator.hasNext(), "Should find the " + i + "th record");
						record = iterator.next();
						assertEquals("123456", toString(record.get(TaxPayerId.name())));
						assertEquals("2021", toString(record.get(TaxYear.name())));
					}

					assertFalse(iterator.hasNext(), "Should not find any more records!");
				}

			}
		}
	}

	/**
	 * Performs tests on an electronic invoice where only the product data is grouped. 
	 * The remaining data (issuer, recipient, carrier, products, and totals) are inserted 
	 * directly into the root XML tag. It is expected that all lines reproduce all data, 
	 * except for the product data, which individualizes the data.
	 * @throws Exception
	 */
	@Test
	void invoiceSimpleTest() throws Exception {

		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());

		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XML);
		inputSpec.setInputName("Invoice XML");
		template.addInput(inputSpec);

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("header_id")
		        .withColumnNameExpression("header_id"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("header_series")
		        .withColumnNameExpression("header_series"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("header_issue_date")
		        .withColumnNameExpression("header_issue_date"));
		
		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_tax_id")
		        .withColumnNameExpression("issuer_tax_id"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_name")
		        .withColumnNameExpression("issuer_name"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_address")
		        .withColumnNameExpression("issuer_address"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_city")
		        .withColumnNameExpression("issuer_city"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_state")
		        .withColumnNameExpression("issuer_state"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_zip_code")
		        .withColumnNameExpression("issuer_zip_code"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_phone")
		        .withColumnNameExpression("issuer_phone"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_email")
		        .withColumnNameExpression("issuer_email"));
		
		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_tax_id")
		        .withColumnNameExpression("receiver_tax_id"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_name")
		        .withColumnNameExpression("receiver_name"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_address")
		        .withColumnNameExpression("receiver_address"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_city")
		        .withColumnNameExpression("receiver_city"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_state")
		        .withColumnNameExpression("receiver_state"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_zip_code")
		        .withColumnNameExpression("receiver_zip_code"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_phone")
		        .withColumnNameExpression("receiver_phone"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_email")
		        .withColumnNameExpression("receiver_email"));
		
		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_tax_id")
		        .withColumnNameExpression("carrier_tax_id"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_name")
		        .withColumnNameExpression("carrier_name"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_address")
		        .withColumnNameExpression("carrier_address"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_vehicle_plate")
		        .withColumnNameExpression("carrier_vehicle_plate"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_state")
		        .withColumnNameExpression("carrier_state"));
		
		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_net_value")
		        .withColumnNameExpression("total_net_value"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_iva_amount")
		        .withColumnNameExpression("total_iva_amount"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_pis_amount")
		        .withColumnNameExpression("total_pis_amount"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_cofins_amount")
		        .withColumnNameExpression("total_cofins_amount"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_payable_amount")
		        .withColumnNameExpression("total_payable_amount"));		
		
		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_sku")
		        .withPathExpression("Product.product_sku"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_description")
		        .withPathExpression("Product.product_description"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_unit_price")
		        .withPathExpression("Product.product_unit_price"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_quantity")
		        .withPathExpression("Product.product_quantity"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_discount")
		        .withPathExpression("Product.product_discount"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_total")
		        .withPathExpression("Product.product_total"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_iva")
		        .withPathExpression("Product.product_iva"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_pis")
		        .withPathExpression("Product.product_pis"));

		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_cofins")
		        .withPathExpression("Product.product_cofins"));

		for (String resource : new String[] { "/samples/invoice01.xml" }) {

			System.out.println("Testing with: " + resource);

			Resource sampleFile = new ClassPathResource(resource);
			assertTrue(sampleFile.exists());

			try (XMLParser parser = new XMLParser();) {

				parser.setPath(sampleFile.getFile().toPath());
				parser.setDocumentInputSpec(inputSpec);
				parser.start();

				try (DataIterator iterator = parser.iterator();) {

					for (int i = 0; i < 5; i++) {
						assertTrue(iterator.hasNext(), "Should find the " + i + "th record");
						Map<String, Object> record = iterator.next();						
						record.forEach((key,value)->assertNotNull(value,key));					
					}

					assertFalse(iterator.hasNext(), "Should not find any more records!");
				}

			}
		}
	}
	
	/**
	 * Performs tests on an electronic invoice where the data is grouped (issuer, recipient, carrier, products, and totals). 
	 * It is expected that all lines reproduce all data, except for the product data, which individualizes the data.
	 * @throws Exception
	 */
//	@Test
//	void invoiceHierarquicalTest() throws Exception {
//
//		TemplateArchetype archetype = new ChartOfAccountsArchetype();
//		DocumentTemplate template = new DocumentTemplate();
//		template.setArchetype(archetype.getName());
//		template.setFields(archetype.getRequiredFields());
//
//		DocumentInput inputSpec = new DocumentInput();
//		inputSpec.setFormat(DocumentFormat.XML);
//		inputSpec.setInputName("Invoice XML");
//		template.addInput(inputSpec);
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("header_id")
//		        .withColumnNameExpression("id"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("header_series")
//		        .withColumnNameExpression("series"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("header_issue_date")
//		        .withColumnNameExpression("issue_date"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_tax_id")
//		        .withPathExpression("Issuer.tax_id"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_name")
//		        .withPathExpression("Issuer.name"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_address")
//		        .withPathExpression("Issuer.address"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_city")
//		        .withPathExpression("Issuer.city"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_state")
//		        .withPathExpression("Issuer.state"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("issuer_zip_code")
//		        .withPathExpression("Issuer.zip_code"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_tax_id")
//		        .withPathExpression("Receiver.tax_id"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_name")
//		        .withPathExpression("Receiver.name"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_address")
//		        .withPathExpression("Receiver.address"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("receiver_email")
//		        .withPathExpression("Receiver.email"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_tax_id")
//		        .withPathExpression("Carrier.tax_id"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_name")
//		        .withPathExpression("Carrier.name"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("carrier_vehicle_plate")
//		        .withPathExpression("Carrier.vehicle_plate"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_sku")
//		        .withPathExpression("Items.Product.sku"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_description")
//		        .withPathExpression("Items.Product.description"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_quantity")
//		        .withPathExpression("Items.Product.quantity"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_unit_price")
//		        .withPathExpression("Items.Product.unit_price"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_total")
//		        .withPathExpression("Items.Product.total"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_iva")
//		        .withPathExpression("Items.Product.iva"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_pis")
//		        .withPathExpression("Items.Product.pis"));		
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("product_cofins")
//		        .withPathExpression("Items.Product.cofins"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_net_value")
//		        .withPathExpression("Totals.net_value"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_iva")
//		        .withPathExpression("Totals.iva_amount"));
//
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_pis")
//		        .withPathExpression("Totals.pis_amount"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_cofins")
//		        .withPathExpression("Totals.cofins_amount"));
//		
//		inputSpec.addField(new DocumentInputFieldMapping().withFieldName("total_payable")
//		        .withPathExpression("Totals.payable_amount"));		
//
//		for (String resource : new String[] { "/samples/invoice10.xml" }) {
//
//			System.out.println("Testing with: " + resource);
//
//			Resource sampleFile = new ClassPathResource(resource);
//			assertTrue(sampleFile.exists());
//
//			try (XMLParser parser = new XMLParser();) {
//
//				parser.setPath(sampleFile.getFile().toPath());
//				parser.setDocumentInputSpec(inputSpec);
//				parser.start();
//
//				try (DataIterator iterator = parser.iterator();) {
//
//					for (int i = 0; i < 5; i++) {
//						assertTrue(iterator.hasNext(), "Should find the " + i + "th record");
//						Map<String, Object> record = iterator.next();						
//						record.forEach((key,value)->assertNotNull(value,key));					
//					}
//
//					assertFalse(iterator.hasNext(), "Should not find any more records!");
//				}
//
//			}
//		}
//	}

	public static String toString(Object value) {
		if ( value == null )
			return null;
		if (value instanceof Number) {
			return String.valueOf(((Number) value).longValue());
		} else {
			return value.toString();
		}
	}
}
